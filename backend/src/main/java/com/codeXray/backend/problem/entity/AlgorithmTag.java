package com.codeXray.backend.problem.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "algorithm_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
