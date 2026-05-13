package com.flowboard.list.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ListRequestDTO {

    @NotNull(message = "boardId is required")
    private Long boardId;

    @NotBlank(message = "name is required")
    private String name;

    private String color;
}
