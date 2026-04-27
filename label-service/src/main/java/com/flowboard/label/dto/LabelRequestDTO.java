package com.flowboard.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LabelRequestDTO {

    @NotNull(message = "boardId is required")
    private Long boardId;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "color is required")
    private String color;
}

