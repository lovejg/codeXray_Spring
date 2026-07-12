package com.codeXray.backend.solution.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "memos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK 주인은 Memo. unique=true라서 한 풀이당 메모 하나
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solution_id", nullable = false, unique = true)
    private Solution solution;

    @Column(columnDefinition = "TEXT")
    private String wrongReason;

    @Column(columnDefinition = "TEXT")
    private String logic;

    @Column(columnDefinition = "TEXT")
    private String keyFunctions;

    @Column(columnDefinition = "TEXT")
    private String freeNote;

    @Builder
    public Memo(Solution solution, String wrongReason, String logic, String keyFunctions, String freeNote) {
        this.solution = solution;
        this.wrongReason = wrongReason;
        this.logic = logic;
        this.keyFunctions = keyFunctions;
        this.freeNote = freeNote;
    }

    public void update(String wrongReason, String logic, String keyFunctions, String freeNote) {
        this.wrongReason = wrongReason;
        this.logic = logic;
        this.keyFunctions = keyFunctions;
        this.freeNote = freeNote;
    }
}
