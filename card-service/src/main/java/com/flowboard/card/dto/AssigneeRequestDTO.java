package com.flowboard.card.dto;

import lombok.Data;

@Data
public class AssigneeRequestDTO {
    // Nullable — send null to unassign
    private Long assigneeId;
}
