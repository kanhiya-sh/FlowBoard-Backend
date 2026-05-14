package com.flowboard.workspace;

import com.flowboard.workspace.dto.NotificationEventDTO;
import com.flowboard.workspace.messaging.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private NotificationPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new NotificationPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", "ex");
        ReflectionTestUtils.setField(publisher, "routingKey", "rk");
    }

    @Test
    void publish_sendsToRabbit() {
        NotificationEventDTO event = NotificationEventDTO.builder()
                .actorId(1L).recipientId(2L).type("ASSIGNMENT").build();
        publisher.publish(event);
        verify(rabbitTemplate).convertAndSend(eq("ex"), eq("rk"), any(Object.class));
    }

    @Test
    void publish_rabbitFails_swallowed() {
        doThrow(new RuntimeException("down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        // should NOT throw
        publisher.publish(NotificationEventDTO.builder().type("X").build());
    }
}
