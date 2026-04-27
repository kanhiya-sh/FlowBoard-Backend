package com.flowboard.card.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ReorderCardsRequestDTO {
    @NotNull
    @NotEmpty(message = "orderedCardIds must not be empty")
    private List<Long> orderedCardIds;
}
