package com.codeXray.backend.notification.service;

import com.codeXray.backend.notification.dto.NotificationListResponse;
import com.codeXray.backend.notification.dto.NotificationResponse;
import com.codeXray.backend.notification.entity.Notification;
import com.codeXray.backend.notification.entity.NotificationType;
import com.codeXray.backend.notification.repository.NotificationRepository;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ─── 다른 서비스가 호출하는 발행 API ───────────────────
    // 자기 자신/익명 대상 알림은 스킵.
    @Transactional
    public void create(Long userId, NotificationType type, Map<String, Object> payload, Long actorId) {
        if (userId == null) return;                             // 익명화된 글의 작성자 등
        if (actorId != null && actorId.equals(userId)) return;  // 내 행동으로 나에게 알림 X
        notificationRepository.save(Notification.builder()
                .userId(userId).type(type).payload(payload).build());
    }

    @Transactional
    public void create(Long userId, NotificationType type, Map<String, Object> payload) {
        create(userId, type, payload, null);
    }

    // 모든 관리자에게 broadcast (신고자 본인이 관리자면 제외)
    @Transactional
    public void createForAllAdmins(NotificationType type, Map<String, Object> payload, Long excludeUserId) {
        List<Notification> batch = userRepository.findIdsByRole(UserRole.ADMIN).stream()
                .filter(id -> !id.equals(excludeUserId))
                .map(id -> Notification.builder().userId(id).type(type).payload(payload).build())
                .toList();
        if (!batch.isEmpty()) notificationRepository.saveAll(batch);
    }

    // ─── 사용자 대면 조회/처리 ─────────────────────────────
    public NotificationListResponse list(Long userId, boolean onlyUnread, Long cursor, Integer limit) {
        int size = (limit == null) ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        Pageable pageable = PageRequest.of(0, size + 1); // hasNext 판별용으로 1개 더

        List<Notification> rows;
        if (onlyUnread) {
            rows = (cursor == null)
                    ? notificationRepository.findByUserIdAndIsReadFalseOrderByIdDesc(userId, pageable)
                    : notificationRepository.findByUserIdAndIsReadFalseAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        } else {
            rows = (cursor == null)
                    ? notificationRepository.findByUserIdOrderByIdDesc(userId, pageable)
                    : notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        }

        boolean hasNext = rows.size() > size;
        List<Notification> page = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;

        List<NotificationResponse> items = page.stream().map(NotificationResponse::from).toList();
        return new NotificationListResponse(items, nextCursor);
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    @Transactional
    public void markRead(Long userId, List<Long> ids) {
        if (ids.isEmpty()) return;
        notificationRepository.markRead(userId, ids, LocalDateTime.now());
    }

    @Transactional
    public void deleteOne(Long userId, Long id) {
        notificationRepository.deleteByIdAndUserId(id, userId); // 남의 알림이면 0건 삭제
    }
}
