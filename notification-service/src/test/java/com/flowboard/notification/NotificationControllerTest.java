package com.flowboard.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.notification.client.AuthServiceClient;
import com.flowboard.notification.controller.NotificationController;
import com.flowboard.notification.dto.BulkNotificationRequestDTO;
import com.flowboard.notification.dto.NotificationRequestDTO;
import com.flowboard.notification.dto.NotificationResponseDTO;
import com.flowboard.notification.dto.UserResponseDTO;
import com.flowboard.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NotificationService notificationService;
    @MockBean private AuthServiceClient authServiceClient;

    private UserResponseDTO mockUser(Long id) {
        UserResponseDTO u = new UserResponseDTO();
        u.setUserId(id);
        u.setEmail("alice@test.com");
        return u;
    }

    private NotificationResponseDTO mockNotif(Long id) {
        return NotificationResponseDTO.builder()
                .notificationId(id)
                .recipientId(1L)
                .type("COMMENT")
                .message("msg")
                .isRead(false)
                .build();
    }

    // ─── Retrieval ───────────────────────────────────────────────────────────────

    @Test
    void getMyNotifications_returnsList() throws Exception {
        when(authServiceClient.getUserByEmail("alice@test.com")).thenReturn(mockUser(1L));
        when(notificationService.getByRecipient(1L))
                .thenReturn(List.of(mockNotif(10L), mockNotif(11L)));

        mockMvc.perform(get("/notifications").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getMyNotifications_whenNoEmailAttr_returns503() throws Exception {
        // No userEmail attribute set — controller throws IllegalStateException → handled as 503
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getMyNotifications_whenAuthReturnsNull_returns503() throws Exception {
        when(authServiceClient.getUserByEmail(any())).thenReturn(null);

        mockMvc.perform(get("/notifications").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getMyNotifications_whenAuthThrows_returns503() throws Exception {
        when(authServiceClient.getUserByEmail(any()))
                .thenThrow(new RuntimeException("Feign error"));

        mockMvc.perform(get("/notifications").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getUnreadNotifications_returnsList() throws Exception {
        when(authServiceClient.getUserByEmail("alice@test.com")).thenReturn(mockUser(1L));
        when(notificationService.getUnreadByRecipient(1L))
                .thenReturn(List.of(mockNotif(10L)));

        mockMvc.perform(get("/notifications/unread").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUnreadCount_returnsCount() throws Exception {
        when(authServiceClient.getUserByEmail("alice@test.com")).thenReturn(mockUser(1L));
        when(notificationService.getUnreadCount(1L)).thenReturn(5L);

        mockMvc.perform(get("/notifications/unread/count").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getByRecipient_pathVariable_returnsList() throws Exception {
        when(notificationService.getByRecipient(42L))
                .thenReturn(List.of(mockNotif(1L)));

        mockMvc.perform(get("/notifications/recipient/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ─── Read state ──────────────────────────────────────────────────────────────

    @Test
    void markAsRead_returnsUpdatedNotif() throws Exception {
        when(notificationService.markAsRead(5L)).thenReturn(mockNotif(5L));

        mockMvc.perform(put("/notifications/5/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(5));
    }

    @Test
    void markAllRead_returnsOk() throws Exception {
        when(authServiceClient.getUserByEmail("alice@test.com")).thenReturn(mockUser(1L));
        doNothing().when(notificationService).markAllRead(1L);

        mockMvc.perform(put("/notifications/read-all").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("All notifications marked as read"));

        verify(notificationService).markAllRead(1L);
    }

    @Test
    void deleteRead_returnsOk() throws Exception {
        when(authServiceClient.getUserByEmail("alice@test.com")).thenReturn(mockUser(1L));
        doNothing().when(notificationService).deleteRead(1L);

        mockMvc.perform(delete("/notifications/read").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk());

        verify(notificationService).deleteRead(1L);
    }

    @Test
    void deleteNotification_returnsOk() throws Exception {
        doNothing().when(notificationService).deleteNotification(7L);

        mockMvc.perform(delete("/notifications/7"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification deleted successfully"));
    }

    // ─── Send ────────────────────────────────────────────────────────────────────

    @Test
    void send_validBody_returnsCreatedNotif() throws Exception {
        NotificationRequestDTO req = new NotificationRequestDTO();
        req.setRecipientId(1L);
        req.setType("COMMENT");
        req.setMessage("hello");

        when(notificationService.send(any())).thenReturn(mockNotif(99L));

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(99));
    }

    @Test
    void send_invalidBody_returns400() throws Exception {
        // Missing required fields (recipientId, type, message)
        NotificationRequestDTO req = new NotificationRequestDTO();

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendBulk_returnsListOfCreatedNotifs() throws Exception {
        NotificationRequestDTO r1 = new NotificationRequestDTO();
        r1.setRecipientId(1L); r1.setType("COMMENT"); r1.setMessage("a");
        NotificationRequestDTO r2 = new NotificationRequestDTO();
        r2.setRecipientId(2L); r2.setType("ASSIGNMENT"); r2.setMessage("b");

        BulkNotificationRequestDTO bulk = new BulkNotificationRequestDTO();
        bulk.setNotifications(List.of(r1, r2));

        when(notificationService.sendBulk(any()))
                .thenReturn(List.of(mockNotif(1L), mockNotif(2L)));

        mockMvc.perform(post("/notifications/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulk)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAll_returnsAllNotifs() throws Exception {
        when(notificationService.getAll())
                .thenReturn(List.of(mockNotif(1L), mockNotif(2L), mockNotif(3L)));

        mockMvc.perform(get("/notifications/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ─── Internal ────────────────────────────────────────────────────────────────

    @Test
    void sendInternal_returnsCreatedNotif() throws Exception {
        NotificationRequestDTO req = new NotificationRequestDTO();
        req.setRecipientId(1L);
        req.setType("COMMENT");
        req.setMessage("internal");

        when(notificationService.send(any())).thenReturn(mockNotif(50L));

        mockMvc.perform(post("/notifications/internal/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(50));
    }

    @Test
    void getUnreadCountInternal_returnsCount() throws Exception {
        when(notificationService.getUnreadCount(7L)).thenReturn(3L);

        mockMvc.perform(get("/notifications/internal/unread/7/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }
}
