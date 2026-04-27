package com.flowboard.card.serviceImpl;

import com.flowboard.card.dto.*;
import com.flowboard.card.entity.Card;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import com.flowboard.card.exception.ResourceNotFoundException;
import com.flowboard.card.exception.UnauthorizedException;
import com.flowboard.card.repository.CardRepository;
import com.flowboard.card.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${board.service.url}")
    private String boardServiceUrl;

    @Value("${list.service.url}")
    private String listServiceUrl;

    // Internal Helpers
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
            log.error("Auth service DOWN: {}", e.getMessage());
            throw new IllegalStateException("Auth service is currently unavailable.");
        }
        catch (HttpClientErrorException e) {
            log.error("Auth service error for email {}: {}", email, e.getStatusCode());
            throw new UnauthorizedException("User not found in Auth service");
        }
        catch (Exception e) {
            log.error("Unexpected error calling Auth service: {}", e.getMessage());
            throw new IllegalStateException("Auth service error: " + e.getMessage());
        }
    }

    private BoardMemberCheckDTO checkBoardMembership(Long boardId, Long userId) {
        String url = boardServiceUrl + "/boards/internal/" + boardId + "/members/" + userId + "/check";
        try {
            BoardMemberCheckDTO result = restTemplate.getForObject(url, BoardMemberCheckDTO.class);
            if (result == null) {
                log.warn("Board service returned null for board {} user {}", boardId, userId);
                return new BoardMemberCheckDTO(false, null);
            }
            log.debug("Board membership check board={} user={}: isMember={}, role={}",
                    boardId, userId, result.isMember(), result.getRole());
            return result;
        } catch (ResourceAccessException e) {
            log.error("Board service DOWN: {}", e.getMessage());
            throw new IllegalStateException("Board service is currently unavailable.");
        } catch (HttpClientErrorException e) {
            log.error("Board service error board={} user={}: {}", boardId, userId, e.getStatusCode());
            return new BoardMemberCheckDTO(false, null);
        } catch (Exception e) {
            log.error("Unexpected error calling Board service: {}", e.getMessage());
            throw new IllegalStateException("Board service error: " + e.getMessage());
        }
    }

    private ListResponseDTO verifyListExists(Long listId) {
        String url = listServiceUrl + "/lists/internal/" + listId + "/exists";
        try {
            ListResponseDTO list = restTemplate.getForObject(url, ListResponseDTO.class);
            if (list == null) {
                throw new ResourceNotFoundException("List not found with id: " + listId);
            }
            return list;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("List not found with id: " + listId);
        } catch (ResourceAccessException e) {
            log.error("List service DOWN: {}", e.getMessage());
            throw new IllegalStateException("List service is currently unavailable.");
        } catch (Exception e) {
            log.error("Unexpected error calling List service: {}", e.getMessage());
            throw new IllegalStateException("List service error: " + e.getMessage());
        }
    }

    private void ensureBoardMember(Long boardId, Long userId) {
        BoardMemberCheckDTO m = checkBoardMembership(boardId, userId);
        if (!m.isMember()) {
            throw new UnauthorizedException("You are not a member of board " + boardId);
        }
    }

    private void ensureBoardAdmin(Long boardId, Long userId) {
        BoardMemberCheckDTO m = checkBoardMembership(boardId, userId);
        if (!m.isMember()) {
            throw new UnauthorizedException("You are not a member of board " + boardId);
        }
        if (!"ADMIN".equalsIgnoreCase(m.getRole())) {
            throw new UnauthorizedException("You must be a board ADMIN to perform this action");
        }
    }

    private int nextPosition(Long listId) {
        return cardRepository.findMaxPositionByListId(listId)
                .map(max -> max + 1)
                .orElse(1);
    }

    // CRUD
    @Override
    @Transactional
    public Card createCard(CardRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);

        // Verify the list exists and get its boardId for membership check
        ListResponseDTO list = verifyListExists(dto.getListId());

        if (list.getIsArchived() != null && list.getIsArchived()) {
            throw new IllegalStateException("Cannot create a card in an archived list");
        }

        // Use the boardId from the list (source of truth) rather than the DTO
        Long boardId = list.getBoardId();
        ensureBoardMember(boardId, userId);

        int position = nextPosition(dto.getListId());

        Card card = Card.builder()
                .listId(dto.getListId())
                .boardId(boardId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .position(position)
                .priority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM)
                .status(dto.getStatus() != null ? dto.getStatus() : Status.TO_DO)
                .dueDate(dto.getDueDate())
                .startDate(dto.getStartDate())
                .assigneeId(dto.getAssigneeId())
                .createdById(userId)
                .isArchived(false)
                .coverColor(dto.getCoverColor())
                .build();

        Card saved = cardRepository.save(card);
        log.info("Card created: cardId={} '{}' in listId={} by userId={}", saved.getCardId(), saved.getTitle(), dto.getListId(), userId);
        return saved;
    }

    @Override
    public Card getCardById(Long cardId) {
        return cardRepository.findByCardId(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
    }

    @Override
    public List<Card> getCardsByList(Long listId) {
        return cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(listId);
    }

    @Override
    public List<Card> getCardsByBoard(Long boardId) {
        return cardRepository.findByBoardIdAndIsArchivedFalse(boardId);
    }

    @Override
    public List<Card> getCardsByAssignee(Long assigneeId) {
        return cardRepository.findByAssigneeIdAndIsArchivedFalse(assigneeId);
    }

    @Override
    @Transactional
    public Card updateCard(Long cardId, CardRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Card card = getCardById(cardId);

        if (card.getIsArchived()) {
            throw new IllegalStateException("Cannot update an archived card. Unarchive it first.");
        }

        ensureBoardMember(card.getBoardId(), userId);

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            card.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            card.setDescription(dto.getDescription());
        }
        if (dto.getPriority() != null) {
            card.setPriority(dto.getPriority());
        }
        if (dto.getStatus() != null) {
            card.setStatus(dto.getStatus());
        }
        if (dto.getDueDate() != null) {
            card.setDueDate(dto.getDueDate());
        }
        if (dto.getStartDate() != null) {
            card.setStartDate(dto.getStartDate());
        }
        if (dto.getAssigneeId() != null) {
            card.setAssigneeId(dto.getAssigneeId());
        }
        if (dto.getCoverColor() != null) {
            card.setCoverColor(dto.getCoverColor());
        }

        Card updated = cardRepository.save(card);
        log.info("Card updated: cardId={} by userId={}", cardId, userId);
        return updated;
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Card card = getCardById(cardId);

        // Can only delete an archived card (soft-delete first, then hard-delete)
        if (!card.getIsArchived()) {
            throw new IllegalStateException("Card must be archived before permanent deletion");
        }

        ensureBoardMember(card.getBoardId(), userId);
        cardRepository.delete(card);
        log.info("Card permanently deleted: cardId={} by userId={}", cardId, userId);
    }

    // Move / Reorder

    @Override
    @Transactional
    public Card moveCard(Long cardId, MoveCardRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Card card = getCardById(cardId);

        if (card.getIsArchived()) {
            throw new IllegalStateException("Cannot move an archived card");
        }

        Long targetListId = dto.getTargetListId();
        ListResponseDTO targetList = verifyListExists(targetListId);

        if (targetList.getIsArchived() != null && targetList.getIsArchived()) {
            throw new IllegalStateException("Cannot move card to an archived list");
        }

        ensureBoardMember(card.getBoardId(), userId);

        Long sourceListId = card.getListId();

        // Shift cards down in source list to close the gap
        if (!sourceListId.equals(targetListId)) {
            List<Card> sourceCards = cardRepository.findByListIdOrderByPosition(sourceListId);
            int removedPos = card.getPosition();
            for (Card c : sourceCards) {
                if (c.getCardId().equals(cardId)) continue;
                if (c.getPosition() > removedPos) {
                    c.setPosition(c.getPosition() - 1);
                }
            }
            cardRepository.saveAll(sourceCards);
        }

        // Determine target position
        int targetPos;
        if (dto.getPosition() != null && dto.getPosition() > 0) {
            targetPos = dto.getPosition();
            // Shift cards in target list to make room
            List<Card> targetCards = cardRepository.findByListIdOrderByPosition(targetListId);
            for (Card c : targetCards) {
                if (c.getCardId().equals(cardId)) continue;
                if (c.getPosition() >= targetPos) {
                    c.setPosition(c.getPosition() + 1);
                }
            }
            cardRepository.saveAll(targetCards);
        }
        else {
            targetPos = nextPosition(targetListId);
        }
        card.setListId(targetListId);
        card.setBoardId(targetList.getBoardId());
        card.setPosition(targetPos);

        Card moved = cardRepository.save(card);
        log.info("Card moved: cardId={} from listId={} to listId={} position={} by userId={}",
                cardId, sourceListId, targetListId, targetPos, userId);
        return moved;
    }

    @Override
    @Transactional
    public List<Card> reorderCards(Long listId, ReorderCardsRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        ListResponseDTO list = verifyListExists(listId);
        ensureBoardMember(list.getBoardId(), userId);

        List<Long> orderedIds = dto.getOrderedCardIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("orderedCardIds must not be empty");
        }

        List<Card> activeCards = cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(listId);
        List<Long> existingIds = activeCards.stream().map(Card::getCardId).toList();

        for (Long id : orderedIds) {
            if (!existingIds.contains(id)) {
                throw new IllegalArgumentException("Card id=" + id + " does not belong to list " + listId);
            }
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            final int newPosition = i + 1;
            final Long cid = orderedIds.get(i);
            activeCards.stream()
                    .filter(c -> c.getCardId().equals(cid))
                    .findFirst()
                    .ifPresent(c -> c.setPosition(newPosition));
        }

        List<Card> saved = cardRepository.saveAll(activeCards);
        log.info("Reordered {} cards in listId={} by userId={}", saved.size(), listId, userId);
        return saved.stream()
                .sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                .toList();
    }

    // Archival

    @Override
    @Transactional
    public Card archiveCard(Long cardId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Card card = getCardById(cardId);

        if (card.getIsArchived()) {
            throw new IllegalStateException("Card is already archived");
        }

        ensureBoardMember(card.getBoardId(), userId);
        card.setIsArchived(true);

        Card saved = cardRepository.save(card);
        log.info("Card archived: cardId={} by userId={}", cardId, userId);
        return saved;
    }

    @Override
    @Transactional
    public Card unarchiveCard(Long cardId, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Card card = getCardById(cardId);

        if (!card.getIsArchived()) {
            throw new IllegalStateException("Card is not archived");
        }

        ensureBoardMember(card.getBoardId(), userId);

        // Restore at end of original list
        int newPosition = nextPosition(card.getListId());
        card.setIsArchived(false);
        card.setPosition(newPosition);

        Card saved = cardRepository.save(card);
        log.info("Card unarchived: cardId={} restored to position={} by userId={}", cardId, newPosition, userId);
        return saved;
    }

    @Override
    public List<Card> getArchivedCardsByBoard(Long boardId) {
        return cardRepository.findByBoardIdAndIsArchivedTrue(boardId);
    }

    @Override
    public List<Card> getArchivedCardsByList(Long listId) {
        return cardRepository.findByListIdAndIsArchivedTrue(listId);
    }

    // Assignment & Priority & Status

    @Override
    @Transactional
    public Card setAssignee(Long cardId, AssigneeRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Card card = getCardById(cardId);

        if (card.getIsArchived()) {
            throw new IllegalStateException("Cannot update an archived card");
        }

        ensureBoardMember(card.getBoardId(), userId);
        card.setAssigneeId(dto.getAssigneeId()); // null = unassign

        Card saved = cardRepository.save(card);
        log.info("Card assignee set: cardId={} assigneeId={} by userId={}", cardId, dto.getAssigneeId(), userId);
        return saved;
    }

    @Override
    @Transactional
    public Card setPriority(Long cardId, PriorityRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Card card = getCardById(cardId);

        if (card.getIsArchived()) {
            throw new IllegalStateException("Cannot update an archived card");
        }

        ensureBoardMember(card.getBoardId(), userId);
        card.setPriority(dto.getPriority());

        Card saved = cardRepository.save(card);
        log.info("Card priority set: cardId={} priority={} by userId={}", cardId, dto.getPriority(), userId);
        return saved;
    }

    @Override
    @Transactional
    public Card setStatus(Long cardId, StatusRequestDTO dto, String userEmail) {
        Long userId = resolveUserIdFromEmail(userEmail);
        Card card = getCardById(cardId);

        if (card.getIsArchived()) {
            throw new IllegalStateException("Cannot update an archived card");
        }

        ensureBoardMember(card.getBoardId(), userId);
        card.setStatus(dto.getStatus());

        Card saved = cardRepository.save(card);
        log.info("Card status set: cardId={} status={} by userId={}", cardId, dto.getStatus(), userId);
        return saved;
    }

    // Overdue Detection

    @Override
    public List<Card> getOverdueCards() {
        return cardRepository.findOverdueCards(LocalDate.now());
    }

    @Override
    public List<Card> getOverdueCardsByBoard(Long boardId) {
        return cardRepository.findOverdueCardsByBoard(boardId, LocalDate.now());
    }

    // Filtering

    @Override
    public List<Card> getCardsByBoardAndPriority(Long boardId, Priority priority) {
        return cardRepository.findByBoardIdAndPriorityAndIsArchivedFalse(boardId, priority);
    }

    @Override
    public List<Card> getCardsByBoardAndStatus(Long boardId, Status status) {
        return cardRepository.findByBoardIdAndStatusAndIsArchivedFalse(boardId, status);
    }
}
