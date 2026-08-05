package com.codeXray.backend.problem.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "algorithm_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 여러 ProblemTag가 참조하는 태그를 개별 조회하면 N+1 → 최대 100개까지 IN 절로 묶어 로딩
@BatchSize(size = 100)
public class AlgorithmTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Builder
    public AlgorithmTag(String name) {
        this.name = name;
    }
}
