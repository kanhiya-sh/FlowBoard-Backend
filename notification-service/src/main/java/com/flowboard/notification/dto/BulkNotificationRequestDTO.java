package com.flowboard.notification.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BulkNotificationRequestDTO {
    @NotEmpty(message = "notifications list must not be empty")
    private List<NotificationRequestDTO> notifications;
}

