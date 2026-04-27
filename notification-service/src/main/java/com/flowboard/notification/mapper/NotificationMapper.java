package com.flowboard.notification.mapper;

import com.flowboard.notification.dto.NotificationResponseDTO;
import com.flowboard.notification.entity.Notification;

public class NotificationMapper {
    private NotificationMapper() {}
    public static NotificationResponseDTO toResponseDTO(Notification n) {
        return NotificationResponseDTO.builder()
                .notificationId(n.getNotificationId())
                .recipientId(n.getRecipientId())
                .actorId(n.getActorId())
                .type(n.getType().name())
                .message(n.getMessage())
                .title(n.getTitle())
                .relatedId(n.getRelatedId())
                .relatedType(n.getRelatedType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

