package com.flowboard.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.flowboard.workspace.enums.Visibility;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponseDTO {
    private Long workspaceId;
    private String name;
    private String description;
    private Long ownerId;
    private Visibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer memberCount;
}