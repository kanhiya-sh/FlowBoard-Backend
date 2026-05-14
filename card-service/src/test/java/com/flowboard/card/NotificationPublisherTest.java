package com.flowboard.card;

import com.flowboard.card.service.NotificationPublisher;
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
    void sendAssignmentNotification_publishesEvent() {
        publisher.sendAssignmentNotification(1L, 2L, 5L, "Title");
        verify(rabbitTemplate).convertAndSend(eq("ex"), eq("rk"), any(Object.class));
    }

    @Test
    void sendAssignmentNotification_nullRecipient_skips() {
        publisher.sendAssignmentNotification(1L, null, 5L, "Title");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void sendAssignmentNotification_rabbitFails_swallowed() {
        doThrow(new RuntimeException("down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        // should NOT throw
        publisher.sendAssignmentNotification(1L, 2L, 5L, "T");
    }
}
