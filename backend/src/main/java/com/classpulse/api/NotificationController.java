package com.classpulse.api;

import com.classpulse.config.SecurityUtil;
import com.classpulse.notification.Notification;
import com.classpulse.notification.NotificationRepository;
import com.classpulse.notification.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    // --- DTOs ---

    public record NotificationResponse(
            Long id, String type, String title, String message,
            Map<String, Object> data, Boolean isRead,
            LocalDateTime createdAt
    ) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                    n.getDataJson(), n.getIsRead(), n.getCreatedAt()
            );
        }
    }

    public record UnreadCountResponse(long count) {}

    // --- Endpoints ---

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications.stream().map(NotificationResponse::from).toList());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));

        Long userId = SecurityUtil.getCurrentUserId();
        if (!notification.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<Void> markAllRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    /**
     * SSE stream endpoint for real-time notification push.
     * Frontend subscribes to this endpoint to receive notifications in real-time.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Long userId = SecurityUtil.getCurrentUserId();
        return sseEmitterService.createEmitter(userId);
    }
}
