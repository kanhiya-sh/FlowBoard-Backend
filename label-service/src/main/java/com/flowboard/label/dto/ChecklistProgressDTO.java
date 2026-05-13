package com.flowboard.label.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChecklistProgressDTO {
    private Long checklistId;
    private String title;
    private int totalItems;
    private int completedItems;
    private double completionPercentage;
}

