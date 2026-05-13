package com.flowboard.card.dto;

import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CardResponseDTO {

    private Long cardId;
    private Long listId;
    private Long boardId;

    private String title;
    private String description;

    private Integer position;

    private Priority priority;
    private Status status;

    private LocalDate dueDate;
    private LocalDate startDate;

    private Long assigneeId;
    private Long createdById;

    private Boolean isArchived;
    private String coverColor;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed field for convenience
    private Boolean isOverdue;
}
