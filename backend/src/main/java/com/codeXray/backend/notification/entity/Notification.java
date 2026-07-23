package com.codeXray.backend.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 받는 사람. 목록/카운트를 이 값으로 필터하므로 raw Long.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 알림 종류마다 다른 구조의 데이터(postId, postTitle 등)를 jsonb 한 컬럼에.
    // @JdbcTypeCode(JSON): Hibernate가 Map <-> jsonb 직렬화를 담당.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(Long userId, NotificationType type, Map<String, Object> payload) {
        this.userId = userId;
        this.type = type;
        this.payload = payload;
        this.isRead = false;
    }
}
