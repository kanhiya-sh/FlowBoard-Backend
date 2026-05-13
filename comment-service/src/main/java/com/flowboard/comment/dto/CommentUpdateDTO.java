package com.flowboard.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentUpdateDTO {
    @NotBlank(message = "content must not be blank")
    private String content;
}
