package com.flowboard.card.dto;

import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CardRequestDTO {

    @NotNull(message = "listId is required")
    private Long listId;

    @NotNull(message = "boardId is required")
    private Long boardId;

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    private Priority priority;
    private Status status;

    private LocalDate dueDate;
    private LocalDate startDate;

    private Long assigneeId;
    private String coverColor;
}
