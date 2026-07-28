package com.codeXray.backend.ai.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiJobKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiJobStatus status;

    // 컨슈머가 그대로 Claude 에 넘길 프롬프트(요청 시점에 완성해 저장)
    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt", nullable = false, columnDefinition = "TEXT")
    private String userPrompt;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public AiJob(Long userId, AiJobKind kind, String systemPrompt, String userPrompt) {
        this.userId = userId;
        this.kind = kind;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.status = AiJobStatus.PENDING;
    }

    public void markDone(String result) {
        this.result = result;
        this.status = AiJobStatus.DONE;
    }

    public void markFailed(String errorCode) {
        this.errorCode = errorCode;
        this.status = AiJobStatus.FAILED;
    }
}
