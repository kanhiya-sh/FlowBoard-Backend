package com.flowboard.list.serviceImpl;

import com.flowboard.list.dto.*;
import com.flowboard.list.entity.TaskList;
import com.flowboard.list.exception.ResourceNotFoundException;
import com.flowboard.list.exception.UnauthorizedException;
import com.flowboard.list.repository.ListRepository;
import com.flowboard.list.service.ListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListServiceImpl implements ListService {
    private final ListRepository listRepository;
    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${board.service.url}")
    private String boardServiceUrl;

    // Internal Helpers
    // Calls Auth Service to resolve email → userId.
    // Endpoint: GET /auth/internal/users/email/{email}

    private Long resolveUserIdFromEmail(String email) {
        String url = authServiceUrl + "/auth/internal/users/email/" + email;
        try {
            UserResponseDTO user = restTemplate.getForObject(url, UserResponseDTO.class);
            if (user == null || user.getUserId() == null) {
                throw new UnauthorizedException("Could not resolve user from Auth service");
            }
            return user.getUserId();
        }
        catch (UnauthorizedException e) {
            throw e;
        }
        catch (ResourceAccessException e) {
            log.error("Auth service is DOWN - cannot reach {}: {}", url, e.getMessage());
            throw new IllegalStateException("Auth service is currently unavailable. Please try again later.");
        }
        catch (HttpClientErrorException e) {
            log.error("Auth service returned error for email {}: {}", email, e.getStatusCode());
            throw new UnauthorizedException("User not found in Auth service");
        }
        catch (Exception e) {
            log.error("Unexpected error calling Auth service for email {}: {}", email, e.getMessage());
            throw new IllegalStateException("Auth service error: " + e.getMessage());
        }
    }

    /**
     * Calls Board Service to verify that userId is a member of the board.
     * Endpoint: GET /boards/internal/{boardId}/members/{userId}/check
     * This endpoint has permitAll() in board-service security — no JWT needed.
     */
    private BoardMemberCheckDTO checkBoardMembership(Long boardId, Long userId) {
        String url = boardServiceUrl + "/boards/internal/" + boardId + "/members/" + userId + "/check";
        try {
            BoardMemberCheckDTO result = restTemplate.getForObject(url, BoardMemberCheckDTO.class);
            if (result == null) {
                log.warn("Board service returned null for board {} user {}", boardId, userId);
                return new BoardMemberCheckDTO(false, null);
            }
            log.debug("Board membership check for board {} user {}: isMember={}, role={}",
                    boardId, userId, result.isMember(), result.getRole());
            return result;
        }
        catch (ResourceAccessException e) {
            log.error("Board service is DOWN - cannot reach {}: {}", url, e.getMessage());
            throw new IllegalStateException("Board service is currently unavailable. Please try again later.");
        }
        catch (HttpClientErrorException e) {
            log.error("Board service error for board {} user {}: {}", boardId, userId, e.getStatusCode());
            return new BoardMemberCheckDTO(false, null);
        }
        catch (Exception e) {
            log.error("Unexpected error calling Board service: {}", e.getMessage());
            throw new IllegalStateException("Board service error: " + e.getMessage());
        }
    }

    /**
     * Ensures the requesting user is a member of the board that owns this list.
     * Throws UnauthorizedException if not a member.
     */
    private void ensureBoardMember(Long boardId, Long userId) {
        BoardMemberCheckDTO membership = checkBoardMembership(boardId, userId);
        if (!membership.isMember()) {
            throw new UnauthorizedException(
                "You are not a member of board " + boardId + ". Join the board first.");
        }
    }

    /**
     * Ensures the requesting user is a board ADMIN.
     * Throws UnauthorizedException if not an ADMIN.
     */
    private void ensureBoardAdmin(Long boardId, Long userId) {
        BoardMemberCheckDTO membership = checkBoardMembership(boardId, userId);
        if (!membership.isMember()) {
            throw new UnauthorizedException("You are not a member of board " + boardId);
        }
        if (!"ADMIN".equalsIgnoreCase(membership.getRole())) {
            throw new UnauthorizedException(
                "You must be a board ADMIN to perform this action on board " + boardId);
        }
    }

    /**
     * Computes the next available position for a new list in a board.
     * Positions are 1-based (1, 2, 3, ...).
     */
    private int nextPosition(Long boardId) {
        return listRepository.findMaxPositionByBoardId(boardId)
                .map(max -> max + 1)
                .orElse(1);
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TaskList createList(ListRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        log.debug("Creating list '{}' for boardId={} by userId={}", dto.getName(), dto.getBoardId(), userId);

        // Verify caller is a board member
        ensureBoardMember(dto.getBoardId(), userId);

        int position = nextPosition(dto.getBoardId());

        TaskList list = TaskList.builder()
                .boardId(dto.getBoardId())
                .name(dto.getName())
                .position(position)
                .color(dto.getColor())
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TaskList saved = listRepository.save(list);
        log.info("List created: listId={} '{}' at position={} in boardId={}", saved.getListId(), saved.getName(), position, dto.getBoardId());
        return saved;
    }

    @Override
    public TaskList getListById(Long listId) {
        return listRepository.findByListId(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found with id: " + listId));
    }

    @Override
    public List<TaskList> getListsByBoard(Long boardId) {
        return listRepository.findByBoardId(boardId);
    }

    @Override
    public List<TaskList> getListsByBoardOrdered(Long boardId) {
        return listRepository.findByBoardIdOrderByPosition(boardId);
    }

    @Override
    @Transactional
    public TaskList updateList(Long listId, ListRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        TaskList list = getListById(listId);

        if (list.getIsArchived()) {
            throw new IllegalStateException("Cannot update an archived list. Unarchive it first.");
        }

        // Only board members can rename a list
        ensureBoardMember(list.getBoardId(), userId);

        if (dto.getName() != null && !dto.getName().isBlank()) {
            list.setName(dto.getName());
        }
        if (dto.getColor() != null) {
            list.setColor(dto.getColor());
        }
        list.setUpdatedAt(LocalDateTime.now());

        TaskList updated = listRepository.save(list);
        log.info("List updated: listId={} by userId={}", listId, userId);
        return updated;
    }

    @Override
    @Transactional
    public void deleteList(Long listId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        TaskList list = getListById(listId);

        // Only board ADMIN can permanently delete a list
        ensureBoardAdmin(list.getBoardId(), userId);

        listRepository.delete(list);
        log.info("List permanently deleted: listId={} by userId={}", listId, userId);
    }

    // ─── Position Management (Reorder) ────────────────────────────────────────────

    @Override
    @Transactional
    public List<TaskList> reorderLists(Long boardId, ReorderRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);

        // Only board members can reorder (drag-and-drop)
        ensureBoardMember(boardId, userId);

        List<Long> orderedIds = dto.getOrderedListIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("orderedListIds must not be empty");
        }

        log.debug("Reordering {} lists in boardId={} by userId={}", orderedIds.size(), boardId, userId);

        // Validate all list IDs belong to this board
        List<TaskList> allLists = listRepository.findByBoardId(boardId);
        List<Long> existingIds = allLists.stream().map(TaskList::getListId).toList();

        for (Long id : orderedIds) {
            if (!existingIds.contains(id)) {
                throw new IllegalArgumentException("List id=" + id + " does not belong to board " + boardId);
            }
        }

        // Work only on active (non-archived) lists
        List<TaskList> activeLists = allLists.stream()
                .filter(l -> !l.getIsArchived())
                .toList();

        // Update positions only for active lists
        for (int i = 0; i < orderedIds.size(); i++) {
            final int newPosition = i + 1;
            final Long listId = orderedIds.get(i);

            activeLists.stream()
                    .filter(l -> l.getListId().equals(listId))
                    .findFirst()
                    .ifPresent(l -> {
                        l.setPosition(newPosition);
                        l.setUpdatedAt(LocalDateTime.now());
                    });
        }

        // Save only active lists
        List<TaskList> saved = listRepository.saveAll(activeLists);
        log.info("Reordered {} lists in boardId={}", saved.size(), boardId);
        return saved.stream()
                .sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                .toList();
    }

    // Archival

    @Override
    @Transactional
    public TaskList archiveList(Long listId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        TaskList list = getListById(listId);

        if (list.getIsArchived()) {
            throw new IllegalStateException("List is already archived");
        }

        // Any board member can archive a list
        ensureBoardMember(list.getBoardId(), userId);

        list.setIsArchived(true);
        list.setUpdatedAt(LocalDateTime.now());
        TaskList saved = listRepository.save(list);
        log.info("List archived: listId={} by userId={}", listId, userId);
        return saved;
    }

    @Override
    @Transactional
    public TaskList unarchiveList(Long listId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        TaskList list = getListById(listId);

        if (!list.getIsArchived()) {
            throw new IllegalStateException("List is not archived");
        }

        // Any board member can unarchive
        ensureBoardMember(list.getBoardId(), userId);

        // Restore at the end of current list order
        int newPosition = nextPosition(list.getBoardId());
        list.setIsArchived(false);
        list.setPosition(newPosition);
        list.setUpdatedAt(LocalDateTime.now());

        TaskList saved = listRepository.save(list);
        log.info("List unarchived: listId={} restored to position={} by userId={}", listId, newPosition, userId);
        return saved;
    }

    @Override
    public List<TaskList> getArchivedLists(Long boardId) {
        return listRepository.findByBoardIdAndIsArchived(boardId, true);
    }

    // ─── Board Transfer ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TaskList moveList(Long listId, MoveListRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        TaskList list = getListById(listId);

        Long sourceBoardId = list.getBoardId();
        Long targetBoardId = dto.getTargetBoardId();

        if (sourceBoardId.equals(targetBoardId)) {
            throw new IllegalArgumentException("Source and target board are the same. Use reorder instead.");
        }

        // Caller must be ADMIN on the source board
        ensureBoardAdmin(sourceBoardId, userId);
        // Caller must also be a member of the target board
        ensureBoardMember(targetBoardId, userId);

        // Place at end of target board's lists
        int newPosition = nextPosition(targetBoardId);
        list.setBoardId(targetBoardId);
        list.setPosition(newPosition);
        list.setUpdatedAt(LocalDateTime.now());

        TaskList moved = listRepository.save(list);
        log.info("List moved: listId={} from boardId={} to boardId={} at position={} by userId={}",
                listId, sourceBoardId, targetBoardId, newPosition, userId);
        return moved;
    }
}
