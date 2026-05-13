package com.flowboard.notification;

import com.flowboard.notification.dto.*;
import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.enums.NotificationType;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.serviceImpl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private NotificationServiceImpl notificationService;

    private Notification testNotif;

    @BeforeEach
    void setUp() {
        testNotif = Notification.builder()
                .notificationId(1L)
                .recipientId(1L)
                .type(NotificationType.ASSIGNMENT)
                .isRead(false)
                .build();
    }

    @Test
    void getByRecipient_returnsList() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testNotif));
        List<NotificationResponseDTO> result = notificationService.getByRecipient(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getUnreadByRecipient_returnsOnlyUnread() {
        when(notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(1L, false))
                .thenReturn(List.of(testNotif));
        List<NotificationResponseDTO> result = notificationService.getUnreadByRecipient(1L);
        assertEquals(1, result.size());
    }

    @Test
    void markAsRead_updatesNotification() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotif));
        when(notificationRepository.save(any())).thenReturn(testNotif);

        NotificationResponseDTO result = notificationService.markAsRead(1L);
        assertNotNull(result);
        verify(notificationRepository).save(testNotif);
    }

    @Test
    void send_savesNotification() {
        NotificationRequestDTO req = new NotificationRequestDTO();
        req.setRecipientId(1L); req.setType("ASSIGNMENT");
        req.setMessage("You have been assigned"); req.setTitle("New Assignment");

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotif);

        NotificationResponseDTO result = notificationService.send(req);
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void deleteNotification_callsRepository() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotif));
        notificationService.deleteNotification(1L);
        verify(notificationRepository).delete(testNotif);
    }

    @Test
    void getUnreadCount_returnsCorrectCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(1L);
        long count = notificationService.getUnreadCount(1L);
        assertEquals(1L, count);
    }
}
