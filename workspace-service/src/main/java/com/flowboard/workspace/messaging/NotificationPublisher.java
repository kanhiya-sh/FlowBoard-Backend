package com.flowboard.workspace.messaging;

import com.flowboard.workspace.dto.NotificationEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.notification.routing-key}")
    private String routingKey;

    public void publish(NotificationEventDTO event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.debug("Notification event published: type={} actor={} recipient={}",
                    event.getType(), event.getActorId(), event.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to publish notification event: {}", e.getMessage());
        }
    }
}
