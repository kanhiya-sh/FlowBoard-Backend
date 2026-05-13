package com.flowboard.card.controller;

import com.flowboard.card.dto.*;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import com.flowboard.card.mapper.CardMapper;
import com.flowboard.card.service.CardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // ── Helper ──────────────────────────────────────────────────────────────
    private String getEmail(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) throw new IllegalStateException("User not authenticated");
        return email;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * POST /cards
     * Create a new card in a list. Caller must be a board member.
     */
    @PostMapping
    public ResponseEntity<CardResponseDTO> createCard(
            @Valid @RequestBody CardRequestDTO dto,
            HttpServletRequest request) {
        var saved = cardService.createCard(dto, getEmail(request));
        return ResponseEntity.ok(CardMapper.toResponseDTO(saved));
    }

    /**
     * GET /cards/{cardId}
     * Get a single card by ID.
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponseDTO> getCardById(@PathVariable Long cardId) {
        return ResponseEntity.ok(CardMapper.toResponseDTO(cardService.getCardById(cardId)));
    }

    /**
     * GET /cards/list/{listId}
     * Get all active cards in a list, ordered by position.
     */
    @GetMapping("/list/{listId}")
    public ResponseEntity<List<CardResponseDTO>> getCardsByList(@PathVariable Long listId) {
        return ResponseEntity.ok(
                cardService.getCardsByList(listId)
                        .stream()
                        .map(CardMapper::toResponseDTO)
                        .toList()
        );
    }

    /**
     * GET /cards/board/{boardId}
     * Get all active cards on a board.
     */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<CardResponseDTO>> getCardsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(
                cardService.getCardsByBoard(boardId)
                        .stream()
                        .map(CardMapper::toResponseDTO)
                        .toList()
        );
    }

    /**
     * GET /cards/assignee/{assigneeId}
     * Get all active cards assigned to a user.
     */
    @GetMapping("/assignee/{assigneeId}")
    public ResponseEntity<List<CardResponseDTO>> getCardsByAssignee(@PathVariable Long assigneeId) {
        return ResponseEntity.ok(
                cardService.getCardsByAssignee(assigneeId)
                        .stream()
                        .map(CardMapper::toResponseDTO)
                        .toList()
        );
    }

    /**
     * PUT /cards/{cardId}
     * Update card details (title, description, dates, priority, status, coverColor).
     * Caller must be a board member.
     */
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponseDTO> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequestDTO dto,
            HttpServletRequest request) {
        var updated = cardService.updateCard(cardId, dto, getEmail(request));
        return ResponseEntity.ok(CardMapper.toResponseDTO(updated));
    }

    /**
     * DELETE /cards/{cardId}
     * Permanently delete a card (must be archived first).
     * Caller must be a board member.
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<String> deleteCard(
            @PathVariable Long cardId,
            HttpServletRequest request) {
        cardService.deleteCard(cardId, getEmail(request));
        return ResponseEntity.ok("Card permanently deleted");
    }

    // ── Move / Reorder ────────────────────────────────────────────────────────

    /**
     * PUT /cards/{cardId}/move
     * Move a card to a different list (or same list at a specific position).
     * Body: { "targetListId": 5, "position": 2 }
     * Caller must be a board member.
     */
    @PutMapping("/{cardId}/move")
    public ResponseEntity<CardResponseDTO> moveCard(
            @PathVariable Long cardId,
            @Valid @RequestBody MoveCardRequestDTO dto,
            HttpServletRequest request) {
        var moved = cardService.moveCard(cardId, dto, getEmail(request));
        return ResponseEntity.ok(CardMapper.toResponseDTO(moved));
    }

    /**
     * PUT /cards/list/{listId}/reorder
     * Atomically reorder all cards in a list via drag-and-drop.
     * Body: { "orderedCardIds": [3, 1, 2] }
     * Caller must be a board member.
     */
    @PutMapping("/list/{listId}/reorder")
    public ResponseEntity<List<CardResponseDTO>> reorderCards(
            @PathVariable Long listId,
            @Valid @RequestBody ReorderCardsRequestDTO dto,
            HttpServletRequest request) {
        var reordered = cardService.reorderCards(listId, dto, getEmail(request));
        return ResponseEntity.ok(reordered.stream().map(CardMapper::toResponseDTO).toList());
    }

    // ── Archival ─────────────────────────────────────────────────────────────

    /**
     * POST /cards/{cardId}/archive
     * Soft-archive a card. Caller must be a board member.
     */
    @PostMapping("/{cardId}/archive")
    public ResponseEntity<CardResponseDTO> archiveCard(
            @PathVariable Long cardId,
            HttpServletRequest request) {
        var archived = cardService.archiveCard(cardId, getEmail(request));
        return ResponseEntity.ok(CardMapper.toResponseDTO(archived));
    }

    /**
     * POST /cards/{cardId}/unarchive
     * Restore an archived card. Caller must be a board member.
     */
    @PostMapping("/{cardId}/unarchive")
    public ResponseEntity<CardResponseDTO> unarchiveCard(
            @PathVariable Long cardId,
            HttpServletRequest request) {
        var unarchived = cardService.unarchiveCard(cardId, getEmail(request));
        return ResponseEntity.ok(CardMapper.toResponseDTO(unarchived));
    }

    /**
     * GET /cards/board/{boardId}/archived
     * Get all archived cards on a board.
     */
    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<List<CardResponseDTO>> getArchivedCardsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(
                cardService.getArchivedCardsByBoard(boardId)
                        .stream()
                        .map(CardMapper::toResponseDTO)
                        .toList()
        );
    }

    /**
     * GET /cards/list/{listId}/archived
     * Get all archived cards in a list.
     */
    @GetMapping("/list/{listId}/archived")
    public ResponseEntity<List<CardResponseDTO>> getArchivedCardsByList(@PathVariable Long listId) {
        return ResponseEntity.ok(
                cardService.getArchivedCardsByList(listId)
                        .stream()
                        .map(CardMapper::toResponseDTO)
                        .toList()
        );
    }

    // ── Assignment & Priority & Status ────────────────────────────────────────

    /**
     * PUT /cards/{cardId}/assignee
     * Assign or unassign a member to a card.
     * Body: { "assigneeId": 5 } or { "assigneeId": null } to unassign.
     */
    @PutMapping("/{cardId}/assignee")
    public ResponseEntity<CardResponseDTO> setAssignee(
            @PathVariable Long cardId,
            @RequestBody AssigneeRequestDTO dto,
            HttpServletRequest request) {
        var updated = cardService.setAssignee(cardId, dto, getEmail(request));
        return ResponseEntity.ok(CardMapper.toResponseDTO(updated));
    }

    /**
     * PUT /cards/{cardId}/priority
     * Set card priority. Body: { "priority": "HIGH" }
     */
    @PutMapping("/{cardId}/priority")
    public ResponseEntity<CardResponseDTO> setPriority(
            @PathVariable Long cardId,
            @Valid @RequestBody PriorityRequestDTO dto,
            HttpServletRequest request) {
        var updated = cardService.setPriority(cardId, dto, getEmail(request));
        return ResponseEntity.ok(CardMapper.toResponseDTO(updated));
    }

    /**
     * PUT /cards/{cardId}/status
     * Update card status. Body: { "status": "IN_PROGRESS" }
     */
    @PutMapping("/{cardId}/status")
    public ResponseEntity<CardResponseDTO> setStatus(
            @PathVariable Long cardId,
            @Valid @RequestBody StatusRequestDTO dto,
            HttpServletRequest request) {
        var updated = cardService.setStatus(cardId, dto, getEmail(request));
        return ResponseEntity.ok(CardMapper.toResponseDTO(updated));
    }

    // ── Overdue ───────────────────────────────────────────────────────────────

    /**
     * GET /cards/overdue
     * Platform-wide overdue cards (Platform Admin use).
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<CardResponseDTO>> getOverdueCards() {
        return ResponseEntity.ok(
                cardService.getOverdueCards()
                        .stream()
                        .map(CardMapper::toResponseDTO)
                        .toList()
        );
    }

    /**
     * GET /cards/board/{boardId}/overdue
     * Overdue cards on a specific board.
     */
    @GetMapping("/board/{boardId}/overdue")
    public ResponseEntity<List<CardResponseDTO>> getOverdueCardsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(
                cardService.getOverdueCardsByBoard(boardId)
                        .stream()
                        .map(CardMapper::toResponseDTO)
                        .toList()
        );
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    /**
     * GET /cards/board/{boardId}/filter?priority=HIGH
     */
    @GetMapping("/board/{boardId}/filter")
    public ResponseEntity<List<CardResponseDTO>> filterCards(
            @PathVariable Long boardId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Status status) {

        List<CardResponseDTO> result;
        if (priority != null) {
            result = cardService.getCardsByBoardAndPriority(boardId, priority)
                    .stream().map(CardMapper::toResponseDTO).toList();
        } else if (status != null) {
            result = cardService.getCardsByBoardAndStatus(boardId, status)
                    .stream().map(CardMapper::toResponseDTO).toList();
        } else {
            result = cardService.getCardsByBoard(boardId)
                    .stream().map(CardMapper::toResponseDTO).toList();
        }
        return ResponseEntity.ok(result);
    }

    // ── Internal Endpoints (service-to-service, no JWT) ───────────────────────

    /**
     * GET /cards/internal/{cardId}/exists
     * Used by comment-service, label-service etc. to verify a card exists.
     */
    @GetMapping("/internal/{cardId}/exists")
    public ResponseEntity<CardResponseDTO> checkCardExists(@PathVariable Long cardId) {
        return ResponseEntity.ok(CardMapper.toResponseDTO(cardService.getCardById(cardId)));
    }

    /**
     * GET /cards/internal/board/{boardId}/count
     * Used by board-service for analytics.
     */
    @GetMapping("/internal/board/{boardId}/count")
    public ResponseEntity<Long> getCardCountByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(cardService.getCardsByBoard(boardId).stream().count());
    }
}
