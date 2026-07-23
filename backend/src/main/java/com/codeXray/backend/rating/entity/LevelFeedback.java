package com.codeXray.backend.rating.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "level_feedback",
        // 한 유저는 한 문제에 피드백 1개 (upsert 기준)
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(nullable = false)
    private int level; // 사용자가 매긴 체감 난이도 (0~5)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public LevelFeedback(Long userId, Long problemId, int level) {
        this.userId = userId;
        this.problemId = problemId;
        this.level = level;
    }

    public void updateLevel(int level) {
        this.level = level;
    }
}
