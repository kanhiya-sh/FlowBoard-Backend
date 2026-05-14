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

    @Test
    void send_withInvalidType_defaultsToComment() {
        NotificationRequestDTO req = new NotificationRequestDTO();
        req.setRecipientId(1L);
        req.setType("NONEXISTENT_TYPE");
        req.setMessage("msg");
        req.setTitle("title");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.send(req);
        assertNotNull(result);
        assertEquals(NotificationType.COMMENT.name(), result.getType());
    }

    @Test
    void send_withLowercaseType_isParsedCorrectly() {
        NotificationRequestDTO req = new NotificationRequestDTO();
        req.setRecipientId(1L);
        req.setType("assignment");
        req.setMessage("msg");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.send(req);
        assertEquals(NotificationType.ASSIGNMENT.name(), result.getType());
    }

    @Test
    void sendBulk_skipsRequestsWithNullRecipient() {
        NotificationRequestDTO valid = new NotificationRequestDTO();
        valid.setRecipientId(1L);
        valid.setType("COMMENT");
        valid.setMessage("ok");

        NotificationRequestDTO invalid = new NotificationRequestDTO();
        invalid.setRecipientId(null);
        invalid.setType("COMMENT");
        invalid.setMessage("skip");

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotif);

        List<NotificationResponseDTO> result =
                notificationService.sendBulk(List.of(valid, invalid));

        assertEquals(1, result.size());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendBulk_withEmptyList_returnsEmpty() {
        List<NotificationResponseDTO> result = notificationService.sendBulk(List.of());
        assertTrue(result.isEmpty());
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void markAsRead_whenNotFound_throwsResourceNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(
                com.flowboard.notification.exception.ResourceNotFoundException.class,
                () -> notificationService.markAsRead(99L)
        );
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllRead_marksAllUnreadAsRead() {
        Notification n1 = Notification.builder().notificationId(1L).recipientId(1L).isRead(false).build();
        Notification n2 = Notification.builder().notificationId(2L).recipientId(1L).isRead(false).build();
        when(notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(1L, false))
                .thenReturn(List.of(n1, n2));

        notificationService.markAllRead(1L);

        assertTrue(n1.getIsRead());
        assertTrue(n2.getIsRead());
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void markAllRead_whenNoUnread_savesEmptyList() {
        when(notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(1L, false))
                .thenReturn(List.of());

        notificationService.markAllRead(1L);

        verify(notificationRepository).saveAll(List.of());
    }

    @Test
    void deleteRead_callsRepositoryWithCorrectArgs() {
        notificationService.deleteRead(42L);
        verify(notificationRepository).deleteByRecipientIdAndIsRead(42L, true);
    }

    @Test
    void deleteNotification_whenNotFound_throwsResourceNotFound() {
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(
                com.flowboard.notification.exception.ResourceNotFoundException.class,
                () -> notificationService.deleteNotification(404L)
        );
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void getAll_returnsAllMappedToResponseDTO() {
        Notification n1 = Notification.builder().notificationId(1L).recipientId(1L)
                .type(NotificationType.COMMENT).isRead(false).build();
        Notification n2 = Notification.builder().notificationId(2L).recipientId(2L)
                .type(NotificationType.ASSIGNMENT).isRead(true).build();
        when(notificationRepository.findAll()).thenReturn(List.of(n1, n2));

        List<NotificationResponseDTO> result = notificationService.getAll();
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getNotificationId());
        assertEquals(2L, result.get(1).getNotificationId());
    }
}
