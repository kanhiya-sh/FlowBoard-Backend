package com.flowboard.card.dto;

import com.flowboard.card.enums.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PriorityRequestDTO {
    @NotNull(message = "priority is required")
    private Priority priority;
}
