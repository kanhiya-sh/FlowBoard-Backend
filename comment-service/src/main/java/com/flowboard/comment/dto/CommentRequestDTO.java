package com.flowboard.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequestDTO {

    @NotNull(message = "cardId is required")
    private Long cardId;

    @NotBlank(message = "content must not be blank")
    private String content;

    // null = top-level comment; provided = reply
    private Long parentCommentId;
}
