package com.codeXray.backend.problem.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "problem_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"problem_id", "tag_id"}) // 같은 쌍 중복 방지
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private AlgorithmTag tag;

    @Builder
    public ProblemTag(Problem problem, AlgorithmTag tag) {
        this.problem = problem;
        this.tag = tag;
    }
}
