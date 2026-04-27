package com.flowboard.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChecklistRequestDTO {

    @NotNull(message = "cardId is required")
    private Long cardId;

    @NotBlank(message = "title is required")
    private String title;
}

