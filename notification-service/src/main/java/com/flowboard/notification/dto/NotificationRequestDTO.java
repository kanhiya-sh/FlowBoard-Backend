package com.flowboard.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequestDTO {

    @NotNull(message = "recipientId is required")
    private Long recipientId;

    private Long actorId;

    @NotBlank(message = "type is required")
    private String type;

    @NotBlank(message = "message is required")
    private String message;

    private String title;

    private Long relatedId;

    private String relatedType;
}

