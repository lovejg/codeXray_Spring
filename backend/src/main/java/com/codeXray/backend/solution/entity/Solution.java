package com.codeXray.backend.solution.entity;

import com.codeXray.backend.problem.entity.Problem;
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
        name = "solutions",
        // 한 유저는 한 문제에 풀이 1개
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // User 객체를 탐색할 일이 없어 Long

    // 풀이 응답에 문제 제목/태그를 함께 보여줄 거라 탐색이 필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private boolean starred; // 다시 풀어야 할 문제 표시

    // 읽기용(FK 주인은 Memo => 저장/삭제는 Memo 쪽이 담당). 응답에 메모를 실으려고 추가
    @OneToOne(mappedBy = "solution", fetch = FetchType.LAZY)
    private Memo memo;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public Solution(Long userId, Problem problem, String code, String language) {
        this.userId = userId;
        this.problem = problem;
        this.code = code;
        this.language = normalizeLanguage(language);
        this.starred = false;
    }

    // 코드 및 언어 갱신
    public void updateCode(String code, String language) {
        this.code = code;
        this.language = normalizeLanguage(language);
    }

    // 별표 토글
    public void toggleStar() {
        this.starred = !this.starred;
    }

    private static String normalizeLanguage(String language) {
        return (language == null || language.isBlank()) ? "python" : language;
    }
}
