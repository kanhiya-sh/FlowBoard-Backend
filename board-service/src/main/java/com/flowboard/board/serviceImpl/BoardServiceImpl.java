package com.flowboard.board.serviceImpl;

import com.flowboard.board.client.AuthServiceClient;
import com.flowboard.board.client.WorkspaceServiceClient;
import com.flowboard.board.dto.*;
import com.flowboard.board.entity.*;
import com.flowboard.board.enums.BoardRole;
import com.flowboard.board.exception.ResourceNotFoundException;
import com.flowboard.board.exception.UnauthorizedException;
import com.flowboard.board.repository.*;
import com.flowboard.board.service.BoardService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final AuthServiceClient authServiceClient;
    private final WorkspaceServiceClient workspaceServiceClient;

    // ─── Internal helpers ─────────────────────────────────────────────────────────────

    /**
     * Calls Auth Service (via Feign + Eureka) to resolve email → userId.
     */
    private Long resolveUserIdFromEmail(String email) {
        try {
            UserResponseDTO user = authServiceClient.getUserByEmail(email);
            if (user == null || user.getUserId() == null) {
                throw new UnauthorizedException("Could not resolve user from Auth service");
            }
            return user.getUserId();
        } catch (UnauthorizedException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            log.error("Auth service: user not found for email {}", email);
            throw new UnauthorizedException("User not found in Auth service");
        } catch (FeignException e) {
            log.error("Auth service call failed for email {}: {}", email, e.getMessage());
            throw new IllegalStateException("Auth service is currently unavailable.");
        } catch (Exception e) {
            log.error("Unexpected error calling Auth service for email {}: {}", email, e.getMessage());
            throw new IllegalStateException("Auth service error: " + e.getMessage());
        }
    }

    /**
     * Calls Workspace Service (via Feign + Eureka) to check membership.
     */
    private WorkspaceMemberCheckDTO checkWorkspaceMembership(Long workspaceId, Long userId) {
        try {
            WorkspaceMemberCheckDTO result = workspaceServiceClient.checkMembership(workspaceId, userId);
            if (result == null) {
                return new WorkspaceMemberCheckDTO(false, null);
            }
            return result;
        } catch (FeignException.NotFound e) {
            return new WorkspaceMemberCheckDTO(false, null);
        } catch (FeignException e) {
            log.error("Workspace service call failed: {}", e.getMessage());
            throw new IllegalStateException("Workspace service is currently unavailable.");
        } catch (Exception e) {
            log.error("Unexpected error calling Workspace service: {}", e.getMessage());
            throw new IllegalStateException("Workspace service error: " + e.getMessage());
        }
    }

    private BoardMemberResponseDTO toMemberDTO(BoardMember m) {
        BoardMemberResponseDTO.BoardMemberResponseDTOBuilder builder = BoardMemberResponseDTO.builder()
                .boardMemberId(m.getBoardMemberId())
                .boardId(m.getBoardId())
                .userId(m.getUserId())
                .role(m.getRole())
                .addedAt(m.getAddedAt());

        // Best-effort enrichment with email/full name from Auth service so the
        // frontend never has to render "User #id". Null-safe: a Feign failure
        // simply leaves the fields blank and the row still ships.
        try {
            UserResponseDTO user = authServiceClient.getUserById(m.getUserId());
            if (user != null) {
                builder.fullName(user.getFullName())
                       .email(user.getEmail())
                       .username(user.getUsername())
                       .avatarUrl(user.getAvatarUrl());
            }
        } catch (Exception e) {
            log.debug("Could not resolve user {} for board-member enrichment: {}",
                    m.getUserId(), e.getMessage());
        }
        return builder.build();
    }

    // ─── Board CRUD ──────────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "boards_by_workspace", key = "#dto.workspaceId")
    public Board createBoard(BoardRequestDTO dto, String userEmail) {
        // Step 1: Resolve email → userId via Auth service
        Long userId = resolveUserIdFromEmail(userEmail);
        log.debug("Creating board for userId={} in workspaceId={}", userId, dto.getWorkspaceId());

        // Step 2: Verify workspace membership via Workspace service
        WorkspaceMemberCheckDTO membership = checkWorkspaceMembership(dto.getWorkspaceId(), userId);
        if (!membership.isMember()) {
            throw new UnauthorizedException(
                "You are not a member of workspace " + dto.getWorkspaceId() +
                ". Join the workspace first before creating a board.");
        }

        // Step 3: Create board
        Board board = Board.builder()
                .workspaceId(dto.getWorkspaceId())
                .name(dto.getName())
                .description(dto.getDescription())
                .background(dto.getBackground())
                .visibility(dto.getVisibility())
                .createdById(userId)
                .isClosed(false)
                .createdAt(LocalDateTime.now())
                .build();

        Board saved = boardRepository.save(board);
        log.info("Board created: boardId={} by userId={}", saved.getBoardId(), userId);

        // Step 4: Add creator as ADMIN member of the board
        BoardMember member = BoardMember.builder()
                .boardId(saved.getBoardId())
                .userId(userId)
                .role(BoardRole.ADMIN)
                .addedAt(LocalDateTime.now())
                .build();
        boardMemberRepository.save(member);

        return saved;
    }

    @Override
    @Cacheable(value = "board_by_id", key = "#boardId")
    public Board getBoardById(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));
    }

    @Override
    @Cacheable(value = "boards_by_workspace", key = "#workspaceId")
    public List<Board> getBoardsByWorkspace(Long workspaceId) {
        return boardRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    @Cacheable(value = "boards_by_user", key = "#userId")
    public List<Board> getBoardsByUser(Long userId) {
        // Get all boards the user is a member of
        List<Long> memberBoardIds = boardMemberRepository.findByUserId(userId)
                .stream().map(BoardMember::getBoardId).toList();

        List<Board> memberBoards = memberBoardIds.isEmpty()
                ? Collections.emptyList()
                : boardRepository.findAllById(memberBoardIds);

        return memberBoards;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "board_by_id", key = "#boardId"),
            @CacheEvict(value = "boards_by_workspace", allEntries = true),
            @CacheEvict(value = "boards_by_user", allEntries = true)
    })
    public Board updateBoard(Long boardId, BoardRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Board board = getBoardById(boardId);

        if (board.getIsClosed()) {
            throw new IllegalStateException("Board is closed and cannot be updated");
        }
        ensureBoardAdminAccess(boardId, userId);

        if (dto.getName() != null && !dto.getName().isBlank()) board.setName(dto.getName());
        if (dto.getDescription() != null) board.setDescription(dto.getDescription());
        if (dto.getBackground() != null) board.setBackground(dto.getBackground());
        if (dto.getVisibility() != null) board.setVisibility(dto.getVisibility());

        board.setUpdatedAt(LocalDateTime.now());
        return boardRepository.save(board);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "board_by_id", key = "#boardId"),
            @CacheEvict(value = "boards_by_workspace", allEntries = true),
            @CacheEvict(value = "boards_by_user", allEntries = true),
            @CacheEvict(value = "board_members", key = "#boardId")
    })
    public void deleteBoard(Long boardId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Board board = getBoardById(boardId);
        ensureBoardAdminAccess(boardId, userId);

        boardMemberRepository.deleteAll(boardMemberRepository.findByBoardId(boardId));
        boardRepository.delete(board);
        log.info("Board deleted: boardId={} by userId={}", boardId, userId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "board_by_id", key = "#boardId"),
            @CacheEvict(value = "boards_by_workspace", allEntries = true)
    })
    public Board closeBoard(Long boardId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Board board = getBoardById(boardId);

        if (board.getIsClosed()) {
            throw new IllegalStateException("Board is already closed");
        }
        ensureBoardAdminAccess(boardId, userId);

        board.setIsClosed(true);
        return boardRepository.save(board);
    }

    // ---- Member Management ----

    @Override
    @Caching(evict = {
            @CacheEvict(value = "board_members", key = "#boardId"),
            @CacheEvict(value = "boards_by_user", key = "#userId")
    })
    public BoardMemberResponseDTO addMember(Long boardId, Long userId, BoardRole role, String userEmail) {
        Long requesterId = resolveUserIdFromEmail(userEmail);
        getBoardById(boardId);
        ensureBoardAdminAccess(boardId, requesterId);

        boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .ifPresent(m -> {
                    throw new IllegalArgumentException("User " + userId + " is already a member of this board");
                });

        BoardMember member = BoardMember.builder()
                .boardId(boardId)
                .userId(userId)
                .role(role)
                .addedAt(LocalDateTime.now())
                .build();

        return toMemberDTO(boardMemberRepository.save(member));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "board_members", key = "#boardId"),
            @CacheEvict(value = "boards_by_user", key = "#userId")
    })
    public void removeMember(Long boardId, Long userId, String userEmail) {
        Long requesterId = resolveUserIdFromEmail(userEmail);
        ensureBoardAdminAccess(boardId, requesterId);

        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in board " + boardId));

        boardMemberRepository.delete(member);
    }

    @Override
    @CacheEvict(value = "board_members", key = "#boardId")
    public BoardMemberResponseDTO updateMemberRole(Long boardId, Long userId, BoardRole role, String userEmail) {
        Long requesterId = resolveUserIdFromEmail(userEmail);
        ensureBoardAdminAccess(boardId, requesterId);

        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in board " + boardId));

        member.setRole(role);
        return toMemberDTO(boardMemberRepository.save(member));
    }

    @Override
    @Cacheable(value = "board_members", key = "#boardId")
    public List<BoardMemberResponseDTO> getBoardMembers(Long boardId) {
        getBoardById(boardId);
        return boardMemberRepository.findByBoardId(boardId)
                .stream().map(this::toMemberDTO).toList();
    }

    @Override
    public List<BoardMemberResponseDTO> getAssignableUsers(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found: " + boardId));

        // Start with explicit board members — this preserves their real roles
        // (ADMIN/MEMBER/OBSERVER) and their enriched email/full-name via the
        // existing toMemberDTO() helper.
        Map<Long, BoardMemberResponseDTO> byUserId = new LinkedHashMap<>();
        for (BoardMember m : boardMemberRepository.findByBoardId(boardId)) {
            byUserId.put(m.getUserId(), toMemberDTO(m));
        }

        // Merge in workspace members who are NOT already listed. We default
        // their role to MEMBER — matches the read-access granted by the
        // workspace-fallback in checkBoardMembership. Failures here are
        // non-fatal: we still return the board-members we already have.
        try {
            List<WorkspaceMemberDTO> wsMembers = workspaceServiceClient.listMembers(board.getWorkspaceId());
            if (wsMembers != null) {
                for (WorkspaceMemberDTO w : wsMembers) {
                    if (w == null || w.getUserId() == null) continue;
                    if (byUserId.containsKey(w.getUserId())) continue;
                    byUserId.put(w.getUserId(), BoardMemberResponseDTO.builder()
                            .userId(w.getUserId())
                            .boardId(boardId)
                            .role(BoardRole.MEMBER)
                            .fullName(w.getFullName())
                            .email(w.getEmail())
                            .username(w.getUsername())
                            .avatarUrl(w.getAvatarUrl())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("getAssignableUsers: workspace members fetch failed for board {} (ws {}): {}",
                    boardId, board.getWorkspaceId(), e.getMessage());
        }

        return new ArrayList<>(byUserId.values());
    }

    @Override
    public BoardMemberCheckResponseDTO checkBoardMembership(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) {
            log.warn("checkBoardMembership: board {} does not exist", boardId);
            return new BoardMemberCheckResponseDTO(false, null);
        }

        // Primary check: explicit board membership wins (preserves ADMIN role).
        var direct = boardMemberRepository.findByBoardIdAndUserId(boardId, userId);
        if (direct.isPresent()) {
            BoardMember m = direct.get();
            log.debug("checkBoardMembership: user {} IS direct member of board {} with role {}",
                    userId, boardId, m.getRole().name());
            return new BoardMemberCheckResponseDTO(true, m.getRole().name());
        }

        // Fallback: a workspace member who hasn't been explicitly added to the
        // board should still get read access to it (open board, view lists, view
        // cards). We never grant ADMIN here — destructive board actions still
        // require explicit board ADMIN membership.
        try {
            WorkspaceMemberCheckDTO wsMembership = checkWorkspaceMembership(board.getWorkspaceId(), userId);
            if (wsMembership.isMember()) {
                log.debug("checkBoardMembership: user {} is workspace member of {} → granting MEMBER access to board {}",
                        userId, board.getWorkspaceId(), boardId);
                return new BoardMemberCheckResponseDTO(true, BoardRole.MEMBER.name());
            }
        } catch (Exception e) {
            // Workspace service hiccup must NOT block direct board members above,
            // and must not silently grant access here. Fall through to "not a member".
            log.warn("checkBoardMembership: workspace fallback failed for user {} board {}: {}",
                    userId, boardId, e.getMessage());
        }

        log.debug("checkBoardMembership: user {} is NOT member of board {} (or its workspace)", userId, boardId);
        return new BoardMemberCheckResponseDTO(false, null);
    }

    // ---- Access Control ----

    private void ensureBoardAdminAccess(Long boardId, Long userId) {
        boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .filter(m -> m.getRole() == BoardRole.ADMIN)
                .orElseThrow(() -> new UnauthorizedException(
                    "You must be a board ADMIN to perform this action on board " + boardId));
    }
}
