package com.flowboard.workspace.dto;

import lombok.Builder;
import lombok.Data;

import com.flowboard.workspace.enums.Visibility;
import java.time.LocalDateTime;

@Data
@Builder
public class WorkspaceResponseDTO {
    private Long workspaceId;
    private String name;
    private String description;
    private Long ownerId;
    private Visibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}