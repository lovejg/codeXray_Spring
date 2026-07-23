package com.codeXray.backend.problem.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "problems")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProblemSource source;

    @Column(nullable = false)
    private int level;

    private Double acceptanceRate;

    private Double adjustedLevel;

    @Enumerated(EnumType.STRING)
    private Tier tier;

    @Column(nullable = false)
    private String link;

    @OneToMany(mappedBy = "problem", fetch = FetchType.LAZY)
    @BatchSize(size = 100) // 여러 문제의 태그를 100개씩 묶어 로딩 → N+1 완화
    private Set<ProblemTag> problemTags = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public Problem(String title, ProblemSource source, int level, Double acceptanceRate, Double adjustedLevel,
                   Tier tier, String link) {
        this.title = title;
        this.source = source;
        this.level = level;
        this.acceptanceRate = acceptanceRate;
        this.adjustedLevel = adjustedLevel;
        this.tier = tier;
        this.link = link;
    }

    // 레이팅 재계산 결과 반영 (dirty checking으로 UPDATE)
    public void applyRating(Double adjustedLevel, Tier tier) {
        this.adjustedLevel = adjustedLevel;
        this.tier = tier;
    }
}
