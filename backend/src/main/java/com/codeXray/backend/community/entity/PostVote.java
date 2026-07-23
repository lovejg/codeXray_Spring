package com.codeXray.backend.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 추천/비추천. 구조는 LevelFeedback 과 동일 패턴(userId+대상id+값, unique로 1인 1표 → upsert).
@Entity
@Table(
        name = "post_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false)
    private int value; // 1 (추천) 또는 -1 (비추천)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public PostVote(Long userId, Long postId, int value) {
        this.userId = userId;
        this.postId = postId;
        this.value = value;
    }

    public void updateValue(int value) {
        this.value = value;
    }
}
