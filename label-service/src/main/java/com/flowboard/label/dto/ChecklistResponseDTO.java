package com.flowboard.label.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChecklistResponseDTO {
    private Long checklistId;
    private Long cardId;
    private String title;
    private Integer position;
    private LocalDateTime createdAt;
    private List<ChecklistItemResponseDTO> items;
    private int totalItems;
    private int completedItems;
}

