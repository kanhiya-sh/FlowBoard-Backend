package com.flowboard.card.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveCardRequestDTO {
    @NotNull(message = "targetListId is required")
    private Long targetListId;

    // Optional: specific position in the target list (null = append at end)
    private Integer position;
}
