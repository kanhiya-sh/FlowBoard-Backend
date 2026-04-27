package com.flowboard.comment.controller;

import com.flowboard.comment.dto.*;
import com.flowboard.comment.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private String getEmail(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) throw new IllegalStateException("User not authenticated");
        return email;
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * POST /comments
     * Add a top-level comment or a threaded reply (parentCommentId != null).
     */
    @PostMapping
    public ResponseEntity<CommentResponseDTO> addComment(
            @Valid @RequestBody CommentRequestDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(commentService.addComment(dto, getEmail(request)));
    }

    /**
     * GET /comments/card/{cardId}
     * Get all top-level (non-deleted) comments for a card, ordered by createdAt ASC.
     * Each comment includes its replyCount.
     *
     * NOTE: This literal path must be declared BEFORE the /{commentId} wildcard
     * so Spring MVC routes it correctly.
     */
    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<CommentResponseDTO>> getByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getByCard(cardId));
    }

    /**
     * GET /comments/card/{cardId}/count
     * Count of active (non-deleted) comments on a card.
     */
    @GetMapping("/card/{cardId}/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getCommentCount(cardId));
    }

    /**
     * GET /comments/{commentId}
     * Get a single comment by ID.
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> getCommentById(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }

    /**
     * GET /comments/{commentId}/replies
     * Get all non-deleted replies to a comment, ordered by createdAt ASC.
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentResponseDTO>> getReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    /**
     * PUT /comments/{commentId}
     * Edit own comment content. Only the author can edit.
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(commentService.updateComment(commentId, dto, getEmail(request)));
    }

    /**
     * DELETE /comments/{commentId}
     * Soft-delete own comment. Only the author can delete.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {
        commentService.deleteComment(commentId, getEmail(request));
        return ResponseEntity.ok("Comment deleted successfully");
    }

    // ─── Internal (service-to-service, no JWT) ─────────────────────────────────

    /**
     * GET /comments/internal/card/{cardId}/count
     * Used by other services to get active comment count for a card.
     * No JWT required — permitted in SecurityConfig.
     */
    @GetMapping("/internal/card/{cardId}/count")
    public ResponseEntity<Long> getCommentCountInternal(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getCommentCount(cardId));
    }
}
