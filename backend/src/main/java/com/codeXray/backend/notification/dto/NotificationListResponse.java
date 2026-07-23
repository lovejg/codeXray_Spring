package com.codeXray.backend.notification.dto;

import java.util.List;

// 커서 페이지네이션 응답. nextCursor 가 null 이면 더 이상 없음.
public record NotificationListResponse(
        List<NotificationResponse> items,
        Long nextCursor
) {
}
