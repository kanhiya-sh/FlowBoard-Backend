package com.flowboard.board.serviceImpl;

import com.flowboard.board.dto.*;
import com.flowboard.board.entity.*;
import com.flowboard.board.enums.BoardRole;
import com.flowboard.board.exception.ResourceNotFoundException;
import com.flowboard.board.exception.UnauthorizedException;
import com.flowboard.board.repository.*;
import com.flowboard.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${workspace.service.url}")
    private String workspaceServiceUrl;

    // ─── Internal helpers ────────────────────────────────────────────────────────

    /**
     * Calls Auth Service to resolve email → full user object with userId.
     * Auth Service endpoint: GET /auth/internal/users/email/{email}
     */
    private Long resolveUserIdFromEmail(String email) {
        String url = authServiceUrl + "/auth/internal/users/email/" + email;
        try {
            UserResponseDTO user = restTemplate.getForObject(url, UserResponseDTO.class);
            if (user == null || user.getUserId() == null) {
                throw new UnauthorizedException("Could not resolve user from Auth service");
            }
            return user.getUserId();
        } catch (UnauthorizedException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Auth service is DOWN - cannot reach {}: {}", url, e.getMessage());
            throw new IllegalStateException("Auth service is currently unavailable. Please try again later.");
        } catch (HttpClientErrorException e) {
            log.error("Auth service returned error for email {}: {}", email, e.getStatusCode());
            throw new UnauthorizedException("User not found in Auth service");
        } catch (Exception e) {
            log.error("Unexpected error calling Auth service for email {}: {}", email, e.getMessage());
            throw new IllegalStateException("Auth service error: " + e.getMessage());
        }
    }

    /**
     * Calls Workspace Service to check if userId is a member of workspaceId.
     * Workspace endpoint: GET /workspaces/internal/{workspaceId}/members/{userId}/check
     *
     * Returns WorkspaceMemberCheckDTO with isMember=true/false and role.
     * This endpoint has permitAll() in workspace security — no JWT needed.
     */
    private WorkspaceMemberCheckDTO checkWorkspaceMembership(Long workspaceId, Long userId) {
        String url = workspaceServiceUrl + "/workspaces/internal/" + workspaceId
                + "/members/" + userId + "/check";
        try {
            WorkspaceMemberCheckDTO result = restTemplate.getForObject(url, WorkspaceMemberCheckDTO.class);
            if (result == null) {
                log.warn("Workspace service returned null for workspace {} user {}", workspaceId, userId);
                return new WorkspaceMemberCheckDTO(false, null);
            }
            log.debug("Membership check for workspace {} user {}: isMember={}, role={}",
                    workspaceId, userId, result.isMember(), result.getRole());
            return result;
        } catch (ResourceAccessException e) {
            log.error("Workspace service is DOWN - cannot reach {}: {}", url, e.getMessage());
            throw new IllegalStateException("Workspace service is currently unavailable. Please try again later.");
        } catch (HttpClientErrorException e) {
            log.error("Workspace service error for workspace {} user {}: {}", workspaceId, userId, e.getStatusCode());
            // 404 means workspace doesn't exist
            return new WorkspaceMemberCheckDTO(false, null);
        } catch (Exception e) {
            log.error("Unexpected error calling Workspace service: {}", e.getMessage());
            throw new IllegalStateException("Workspace service error: " + e.getMessage());
        }
    }

    private BoardMemberResponseDTO toMemberDTO(BoardMember m) {
        return BoardMemberResponseDTO.builder()
                .boardMemberId(m.getBoardMemberId())
                .boardId(m.getBoardId())
                .userId(m.getUserId())
                .role(m.getRole())
                .addedAt(m.getAddedAt())
                .build();
    }

    // ─── Board CRUD ──────────────────────────────────────────────────────────────

    @Override
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
    public Board getBoardById(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));
    }

    @Override
    public List<Board> getBoardsByWorkspace(Long workspaceId) {
        return boardRepository.findByWorkspaceId(workspaceId);
    }

    @Override
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
    public void deleteBoard(Long boardId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Board board = getBoardById(boardId);
        ensureBoardAdminAccess(boardId, userId);

        boardMemberRepository.deleteAll(boardMemberRepository.findByBoardId(boardId));
        boardRepository.delete(board);
        log.info("Board deleted: boardId={} by userId={}", boardId, userId);
    }

    @Override
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
    public void removeMember(Long boardId, Long userId, String userEmail) {
        Long requesterId = resolveUserIdFromEmail(userEmail);
        ensureBoardAdminAccess(boardId, requesterId);

        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in board " + boardId));

        boardMemberRepository.delete(member);
    }

    @Override
    public BoardMemberResponseDTO updateMemberRole(Long boardId, Long userId, BoardRole role, String userEmail) {
        Long requesterId = resolveUserIdFromEmail(userEmail);
        ensureBoardAdminAccess(boardId, requesterId);

        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in board " + boardId));

        member.setRole(role);
        return toMemberDTO(boardMemberRepository.save(member));
    }

    @Override
    public List<BoardMemberResponseDTO> getBoardMembers(Long boardId) {
        getBoardById(boardId);
        return boardMemberRepository.findByBoardId(boardId)
                .stream().map(this::toMemberDTO).toList();
    }

    @Override
    public BoardMemberCheckResponseDTO checkBoardMembership(Long boardId, Long userId) {
        if (!boardRepository.existsById(boardId)) {
            log.warn("checkBoardMembership: board {} does not exist", boardId);
            return new BoardMemberCheckResponseDTO(false, null);
        }
        return boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .map(m -> {
                    log.debug("checkBoardMembership: user {} IS member of board {} with role {}",
                            userId, boardId, m.getRole().name());
                    return new BoardMemberCheckResponseDTO(true, m.getRole().name());
                })
                .orElseGet(() -> {
                    log.debug("checkBoardMembership: user {} is NOT member of board {}", userId, boardId);
                    return new BoardMemberCheckResponseDTO(false, null);
                });
    }

    // ---- Access Control ----

    private void ensureBoardAdminAccess(Long boardId, Long userId) {
        boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .filter(m -> m.getRole() == BoardRole.ADMIN)
                .orElseThrow(() -> new UnauthorizedException(
                    "You must be a board ADMIN to perform this action on board " + boardId));
    }
}
