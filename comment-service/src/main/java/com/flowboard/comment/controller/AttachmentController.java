package com.flowboard.comment.controller;

import com.flowboard.comment.dto.AttachmentRequestDTO;
import com.flowboard.comment.dto.AttachmentResponseDTO;
import com.flowboard.comment.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final CommentService commentService;

    private String getEmail(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) throw new IllegalStateException("User not authenticated");
        return email;
    }

    /**
     * POST /attachments
     * Add a file attachment to a card.
     * Caller must be authenticated (JWT required).
     */
    @PostMapping
    public ResponseEntity<AttachmentResponseDTO> addAttachment(
            @Valid @RequestBody AttachmentRequestDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(commentService.addAttachment(dto, getEmail(request)));
    }

    /**
     * GET /attachments/card/{cardId}
     * Get all attachments for a card, ordered by uploadedAt DESC.
     */
    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<AttachmentResponseDTO>> getAttachmentsByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getAttachmentsByCard(cardId));
    }

    /**
     * DELETE /attachments/{attachmentId}
     * Delete an attachment. Only the uploader can delete their own attachment.
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<String> deleteAttachment(
            @PathVariable Long attachmentId,
            HttpServletRequest request) {
        commentService.deleteAttachment(attachmentId, getEmail(request));
        return ResponseEntity.ok("Attachment deleted successfully");
    }
}
