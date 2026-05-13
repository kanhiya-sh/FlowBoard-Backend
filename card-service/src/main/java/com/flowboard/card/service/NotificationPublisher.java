package com.flowboard.card.service;

import com.flowboard.card.dto.NotificationEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.notification.routing-key}")
    private String routingKey;

    public void sendAssignmentNotification(Long actorId, Long recipientId, Long cardId, String cardTitle) {
        // Unassign / invalid recipient — nothing to publish.
        if (recipientId == null) return;
        // NOTE: self-assignments are intentionally allowed through. The product
        // requirement is to notify the assignee whether they assigned themselves
        // or someone else did it. Callers (setAssignee/updateCard) guarantee
        // we're only invoked when the assignee actually changed, so we don't
        // risk spamming duplicate rows here.

        NotificationEventDTO event = NotificationEventDTO.builder()
                .actorId(actorId)
                .recipientId(recipientId)
                .type("ASSIGNMENT")
                .title("Card Assigned")
                .message("You were assigned to card: " + cardTitle)
                .relatedId(cardId)
                .relatedType("CARD")
                .build();

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Assignment notification published: actor={} recipient={} card={}", actorId, recipientId, cardId);
        } catch (Exception e) {
            log.error("Failed to publish assignment notification: {}", e.getMessage());
        }
    }
}
