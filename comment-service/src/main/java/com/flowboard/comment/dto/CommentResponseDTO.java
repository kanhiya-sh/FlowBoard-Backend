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
    private String content;
    private Long parentCommentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    // Populated on top-level fetches for threading support
    private int replyCount;
}
