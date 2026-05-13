package com.flowboard.notification.repository;

import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(Long recipientId, Boolean isRead);
    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
    List<Notification> findByType(NotificationType type);
    List<Notification> findByRelatedId(Long relatedId);
    void deleteByNotificationId(Long notificationId);
    void deleteByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
}

