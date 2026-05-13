package com.flowboard.notification.controller;

import com.flowboard.notification.client.AuthServiceClient;
import com.flowboard.notification.dto.*;
import com.flowboard.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthServiceClient authServiceClient;

    // Helper — resolves userId from email via Feign + Eureka
    private Long resolveUserId(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) throw new IllegalStateException("User not authenticated");
        try {
            UserResponseDTO user = authServiceClient.getUserByEmail(email);
            if (user == null || user.getUserId() == null) {
                throw new IllegalStateException("Could not resolve user from Auth service");
            }
            return user.getUserId();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to resolve userId: {}", e.getMessage());
            throw new IllegalStateException("Auth service unavailable: " + e.getMessage());
        }
    }

    // Retrieval
    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ResponseEntity.ok(notificationService.getByRecipient(userId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ResponseEntity.ok(notificationService.getUnreadByRecipient(userId));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<NotificationResponseDTO>> getByRecipient(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    // Read State
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PutMapping("/read-all")
    public ResponseEntity<String> markAllRead(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        notificationService.markAllRead(userId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    @DeleteMapping("/read")
    public ResponseEntity<String> deleteRead(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        notificationService.deleteRead(userId);
        return ResponseEntity.ok("Read notifications deleted");
    }

    // Delete
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok("Notification deleted successfully");
    }

    //Send (REST — for direct API testing or admin use)
    @PostMapping
    public ResponseEntity<NotificationResponseDTO> send(
            @Valid @RequestBody NotificationRequestDTO dto) {
        return ResponseEntity.ok(notificationService.send(dto));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationResponseDTO>> sendBulk(
            @Valid @RequestBody BulkNotificationRequestDTO dto) {
        return ResponseEntity.ok(notificationService.sendBulk(dto.getNotifications()));
    }

    // Platform Admin
    @GetMapping("/all")
    public ResponseEntity<List<NotificationResponseDTO>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    // Internal Endpoints (no JWT)
    @PostMapping("/internal/send")
    public ResponseEntity<NotificationResponseDTO> sendInternal(
            @Valid @RequestBody NotificationRequestDTO dto) {
        return ResponseEntity.ok(notificationService.send(dto));
    }

    @GetMapping("/internal/unread/{recipientId}/count")
    public ResponseEntity<Long> getUnreadCountInternal(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId));
    }
}

