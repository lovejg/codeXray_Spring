package com.codeXray.backend.note.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 노트는 문제와 무관한 유저 소유 자산 → 식별/필터 용도라 raw Long
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body; // 마크다운 본문 (코드 블록 포함 가능)

    private String language; // CODE 타입일 때 주 언어 (선택)

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "note_tags", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "tag")
    @BatchSize(size = 100)
    private List<String> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public Note(Long userId, NoteType type, String title, String body, String language, List<String> tags) {
        this.userId = userId;
        this.type = (type == null) ? NoteType.CODE : type;
        this.title = title;
        this.body = body;
        this.language = language;
        if (tags != null) this.tags.addAll(tags);
    }

    // 전체 수정(PUT). 태그는 통째로 교체 — 기존 컬렉션을 clear 후 다시 채워 Hibernate가 변경을 추적하게 함
    public void update(NoteType type, String title, String body, String language, List<String> tags) {
        this.type = (type == null) ? NoteType.CODE : type;
        this.title = title;
        this.body = body;
        this.language = language;
        this.tags.clear();
        if (tags != null) this.tags.addAll(tags);
    }
}
