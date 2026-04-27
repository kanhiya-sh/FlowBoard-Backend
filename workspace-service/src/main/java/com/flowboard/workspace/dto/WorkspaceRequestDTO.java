package com.flowboard.workspace.dto;

import com.flowboard.workspace.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkspaceRequestDTO {

    @NotBlank(message = "Workspace name is required")
    private String name;

    private String description;

    // ownerId now comes from JWT — removed from request body

    @NotNull(message = "Visibility is required")
    private Visibility visibility;
}