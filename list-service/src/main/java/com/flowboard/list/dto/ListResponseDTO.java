package com.flowboard.list.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ListResponseDTO {
    private Long listId;
    private Long boardId;
    private String name;
    private Integer position;
    private String color;
    private Boolean isArchived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
