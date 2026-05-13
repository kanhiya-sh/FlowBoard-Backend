package com.flowboard.list.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveListRequestDTO {
    @NotNull(message = "targetBoardId is required")
    private Long targetBoardId;
}
