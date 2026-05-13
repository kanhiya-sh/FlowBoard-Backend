package com.flowboard.notification.serviceImpl;

import com.flowboard.notification.dto.NotificationRequestDTO;
import com.flowboard.notification.dto.NotificationResponseDTO;
import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.enums.NotificationType;
import com.flowboard.notification.exception.ResourceNotFoundException;
import com.flowboard.notification.mapper.NotificationMapper;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    // Helpers
    private NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification type '{}', defaulting to COMMENT", type);
            return NotificationType.COMMENT;
        }
    }
    private Notification buildFromRequest(NotificationRequestDTO dto) {
        return Notification.builder()
                .recipientId(dto.getRecipientId())
                .actorId(dto.getActorId())
                .type(parseType(dto.getType()))
                .message(dto.getMessage())
                .title(dto.getTitle())
                .relatedId(dto.getRelatedId())
                .relatedType(dto.getRelatedType())
                .isRead(false)
                .build();
    }

    // Send
    @Override
    @Transactional
    public NotificationResponseDTO send(NotificationRequestDTO dto) {
        Notification notification = buildFromRequest(dto);
        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent: id={} type={} recipient={}",
                saved.getNotificationId(), saved.getType(), saved.getRecipientId());
        return NotificationMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public List<NotificationResponseDTO> sendBulk(List<NotificationRequestDTO> dtos) {
        List<NotificationResponseDTO> results = new ArrayList<>();
        for (NotificationRequestDTO dto : dtos) {
            if (dto.getRecipientId() != null) {
                results.add(send(dto));
            }
        }
        log.info("Bulk notification sent: count={}", results.size());
        return results;
    }

    // Read State
    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + notificationId));
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        log.debug("Notification marked as read: id={}", notificationId);
        return NotificationMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void markAllRead(Long recipientId) {
        List<Notification> unread = notificationRepository
                .findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId, false);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        log.info("All notifications marked as read for recipientId={}", recipientId);
    }

    @Override
    @Transactional
    public void deleteRead(Long recipientId) {
        notificationRepository.deleteByRecipientIdAndIsRead(recipientId, true);
        log.info("Read notifications deleted for recipientId={}", recipientId);
    }

    // Retrieval
    @Override
    public List<NotificationResponseDTO> getByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .toList();
    }

    @Override
    public List<NotificationResponseDTO> getUnreadByRecipient(Long recipientId) {
        return notificationRepository
                .findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId, false)
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .toList();
    }

    @Override
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + notificationId));
        notificationRepository.delete(notification);
        log.info("Notification deleted: id={}", notificationId);
    }

    @Override
    public List<NotificationResponseDTO> getAll() {
        return notificationRepository.findAll()
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .toList();
    }
}

