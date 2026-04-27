package com.flowboard.board.dto;

import com.flowboard.board.enums.Visibility;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponseDTO {
    private Long boardId;
    private Long workspaceId;
    private String name;
    private String description;
    private String background;
    private Visibility visibility;
    private Long createdById;
    private Boolean isClosed;
    private LocalDateTime createdAt;
}