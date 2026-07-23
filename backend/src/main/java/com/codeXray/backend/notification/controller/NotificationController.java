package com.codeXray.backend.notification.controller;

import com.codeXray.backend.notification.dto.MarkReadRequest;
import com.codeXray.backend.notification.dto.NotificationListResponse;
import com.codeXray.backend.notification.dto.UnreadCountResponse;
import com.codeXray.backend.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationListResponse list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyUnread,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return notificationService.list(userId, onlyUnread, cursor, limit);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Long userId) {
        return new UnreadCountResponse(notificationService.unreadCount(userId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MarkReadRequest req
    ) {
        notificationService.markRead(userId, req.ids());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOne(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId
    ) {
        notificationService.deleteOne(userId, id);
        return ResponseEntity.noContent().build();
    }
}
