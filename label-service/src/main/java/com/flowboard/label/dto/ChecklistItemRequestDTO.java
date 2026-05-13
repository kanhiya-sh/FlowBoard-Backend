package com.flowboard.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ChecklistItemRequestDTO {

    @NotNull(message = "checklistId is required")
    private Long checklistId;

    @NotBlank(message = "text is required")
    private String text;

    private Long assigneeId;

    private LocalDate dueDate;
}

