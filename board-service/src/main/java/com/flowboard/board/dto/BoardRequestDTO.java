package com.flowboard.board.dto;

import com.flowboard.board.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardRequestDTO {

    @NotNull(message = "Workspace ID is required")
    private Long workspaceId;

    @NotBlank(message = "Board name is required")
    private String name;

    private String description;
    private String background;

    @NotNull(message = "Visibility is required")
    private Visibility visibility;
}