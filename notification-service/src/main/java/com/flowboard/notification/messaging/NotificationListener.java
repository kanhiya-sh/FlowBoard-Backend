package com.flowboard.notification.messaging;

import com.flowboard.notification.dto.NotificationEventDTO;
import com.flowboard.notification.dto.NotificationRequestDTO;
import com.flowboard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {
    private final NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.notification.queue}")
    public void handleNotificationEvent(NotificationEventDTO event) {
        try {
            log.info("Received notification event: type={} actor={} recipient={}",
                    event.getType(), event.getActorId(), event.getRecipientId());
            if (event.getRecipientId() == null) {
                log.debug("Skipping event with null recipientId (broadcast event)");
                return;
            }
            NotificationRequestDTO request = new NotificationRequestDTO();
            request.setRecipientId(event.getRecipientId());
            request.setActorId(event.getActorId());
            request.setType(event.getType() != null ? event.getType() : "COMMENT");
            request.setMessage(event.getMessage() != null ? event.getMessage() : "You have a new notification");
            request.setTitle(event.getTitle());
            request.setRelatedId(event.getRelatedId());
            request.setRelatedType(event.getRelatedType());

            notificationService.send(request);
            log.debug("Notification persisted for recipientId={}", event.getRecipientId());

        }
        catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage(), e);
        }
    }
}

