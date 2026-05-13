package com.flowboard.label.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class LabelResponseDTO {
    private Long labelId;
    private Long boardId;
    private String name;
    private String color;
    private LocalDateTime createdAt;
}

