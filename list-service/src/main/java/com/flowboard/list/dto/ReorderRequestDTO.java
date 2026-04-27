package com.flowboard.list.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ReorderRequestDTO {
    @NotNull(message = "orderedListIds is required")
    private List<Long> orderedListIds;
}
