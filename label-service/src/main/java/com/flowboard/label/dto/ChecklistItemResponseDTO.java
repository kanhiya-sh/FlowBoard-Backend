package com.flowboard.label.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ChecklistItemResponseDTO {
    private Long itemId;
    private Long checklistId;
    private String text;
    private Boolean isCompleted;
    private Long assigneeId;
    private LocalDate dueDate;
}

