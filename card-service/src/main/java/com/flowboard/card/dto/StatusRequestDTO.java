package com.flowboard.card.dto;

import com.flowboard.card.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusRequestDTO {
    @NotNull(message = "status is required")
    private Status status;
}
