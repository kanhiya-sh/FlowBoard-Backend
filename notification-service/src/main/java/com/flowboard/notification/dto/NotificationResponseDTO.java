package com.flowboard.notification.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDTO {
    private Long notificationId;
    private Long recipientId;
    private Long actorId;
    private String type;
    private String message;
    private String title;
    private Long relatedId;
    private String relatedType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

