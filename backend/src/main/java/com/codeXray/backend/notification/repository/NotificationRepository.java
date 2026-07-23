package com.codeXray.backend.notification.repository;

import com.codeXray.backend.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ── 커서 페이지네이션 (id 내림차순 = 최신순). cursor 있으면 그보다 오래된(id 작은) 것부터 ──
    List<Notification> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);
    // onlyUnread 변형
    List<Notification> findByUserIdAndIsReadFalseOrderByIdDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndIsReadFalseAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    // ── 읽음 처리(bulk update). 한 방 UPDATE 라 dirty checking 보다 효율적 ──
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true, n.readAt = :now where n.userId = :userId and n.isRead = false")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true, n.readAt = :now " +
            "where n.userId = :userId and n.isRead = false and n.id in :ids")
    int markRead(@Param("userId") Long userId, @Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    // id + userId 둘 다 맞아야 삭제 → 남의 알림은 못 지움(삭제 0건)
    long deleteByIdAndUserId(Long id, Long userId);

    // 오래된 "읽은" 알림 정리(스케줄러). 삭제 건수 반환.
    @Modifying(clearAutomatically = true)
    @Query("delete from Notification n where n.isRead = true and n.createdAt < :cutoff")
    int deleteReadOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
