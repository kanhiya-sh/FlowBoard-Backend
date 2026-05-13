package com.flowboard.comment.dto;

import lombok.Data;

@Data
public class CardResponseDTO {
    private Long cardId;
    private Long listId;
    private Long boardId;
    private String title;
    private Boolean isArchived;
}
