package com.flowboard.notification;

import com.flowboard.notification.dto.NotificationEventDTO;
import com.flowboard.notification.dto.NotificationRequestDTO;
import com.flowboard.notification.messaging.NotificationListener;
import com.flowboard.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationListenerTest {

    private NotificationService notificationService;
    private NotificationListener listener;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        listener = new NotificationListener(notificationService);
    }

    private NotificationEventDTO event(Long recipientId, String type, String message) {
        NotificationEventDTO e = new NotificationEventDTO();
        e.setRecipientId(recipientId);
        e.setActorId(2L);
        e.setType(type);
        e.setMessage(message);
        e.setTitle("title");
        e.setRelatedId(10L);
        e.setRelatedType("CARD");
        return e;
    }

    @Test
    void handleEvent_persistsNotificationWithAllFields() {
        listener.handleNotificationEvent(event(1L, "ASSIGNMENT", "hello"));

        ArgumentCaptor<NotificationRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationRequestDTO.class);
        verify(notificationService).send(captor.capture());

        NotificationRequestDTO sent = captor.getValue();
        assertEquals(1L, sent.getRecipientId());
        assertEquals(2L, sent.getActorId());
        assertEquals("ASSIGNMENT", sent.getType());
        assertEquals("hello", sent.getMessage());
        assertEquals("title", sent.getTitle());
        assertEquals(10L, sent.getRelatedId());
        assertEquals("CARD", sent.getRelatedType());
    }

    @Test
    void handleEvent_skipsWhenRecipientIsNull() {
        listener.handleNotificationEvent(event(null, "ASSIGNMENT", "hello"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void handleEvent_defaultsTypeToCommentWhenNull() {
        listener.handleNotificationEvent(event(1L, null, "hello"));

        ArgumentCaptor<NotificationRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationRequestDTO.class);
        verify(notificationService).send(captor.capture());
        assertEquals("COMMENT", captor.getValue().getType());
    }

    @Test
    void handleEvent_defaultsMessageWhenNull() {
        listener.handleNotificationEvent(event(1L, "ASSIGNMENT", null));

        ArgumentCaptor<NotificationRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationRequestDTO.class);
        verify(notificationService).send(captor.capture());
        assertEquals("You have a new notification", captor.getValue().getMessage());
    }

    @Test
    void handleEvent_swallowsExceptionsFromService() {
        doThrow(new RuntimeException("DB down"))
                .when(notificationService).send(any());

        // Should NOT throw — listener catches all exceptions
        assertDoesNotThrow(() ->
                listener.handleNotificationEvent(event(1L, "ASSIGNMENT", "hello")));

        verify(notificationService).send(any());
    }
}
