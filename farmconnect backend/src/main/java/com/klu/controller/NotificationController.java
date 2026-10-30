package com.klu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.klu.model.Notification;
import com.klu.repository.NotificationRepository;
import com.klu.service.EmailService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationController(NotificationRepository notificationRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String email = auth.getName();
        List<Notification> list = notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
        long unreadCount = notificationRepository.countByRecipientEmailAndReadFalse(email);

        return ResponseEntity.ok(Map.of(
                "notifications", list,
                "unreadCount", unreadCount
        ));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String id, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return notificationRepository.findById(id).map(notif -> {
            if (auth.getName().equalsIgnoreCase(notif.getRecipientEmail())) {
                notif.setRead(true);
                notificationRepository.save(notif);
                return ResponseEntity.ok(Map.of("success", true, "id", id));
            }
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/test")
    public ResponseEntity<?> sendTestNotification(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        Notification sent = emailService.sendTestNotification(auth.getName(), auth.getName().split("@")[0]);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test notification triggered for " + auth.getName(),
                "notification", sent
        ));
    }
}

