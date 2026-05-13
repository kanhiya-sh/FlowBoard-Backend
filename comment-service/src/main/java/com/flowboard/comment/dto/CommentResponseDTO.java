package com.flowboard.comment.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponseDTO {

    private Long commentId;
    private Long cardId;
    private Long authorId;
    // Populated lazily from Auth service so the frontend can render the user's
    // email/full name without separate lookups. Null-safe: stays null if the
    // Auth call fails so the response payload still ships.
    private String authorEmail;
    private String authorName;
    private String content;
    private Long parentCommentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    // Populated on top-level fetches for threading support
    private int replyCount;
}
