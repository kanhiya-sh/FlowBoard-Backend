package com.flowboard.notification.service;

import com.flowboard.notification.dto.NotificationRequestDTO;
import com.flowboard.notification.dto.NotificationResponseDTO;
import java.util.List;

public interface NotificationService {
    NotificationResponseDTO send(NotificationRequestDTO dto);
    List<NotificationResponseDTO> sendBulk(List<NotificationRequestDTO> dtos);
    NotificationResponseDTO markAsRead(Long notificationId);
    void markAllRead(Long recipientId);
    void deleteRead(Long recipientId);
    List<NotificationResponseDTO> getByRecipient(Long recipientId);
    List<NotificationResponseDTO> getUnreadByRecipient(Long recipientId);
    long getUnreadCount(Long recipientId);
    void deleteNotification(Long notificationId);
    List<NotificationResponseDTO> getAll();
}

