package com.codeXray.backend.notification.dto;

import com.codeXray.backend.notification.entity.Notification;
import com.codeXray.backend.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(
        Long id,
        NotificationType type,
        Map<String, Object> payload,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getPayload(), n.isRead(), n.getReadAt(), n.getCreatedAt());
    }
}
