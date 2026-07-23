package com.codeXray.backend.note.dto;

import com.codeXray.backend.note.entity.Note;
import com.codeXray.backend.note.entity.NoteType;

import java.time.LocalDateTime;
import java.util.List;

public record NoteResponse(
        Long id,
        NoteType type,
        String title,
        String body,
        String language,
        List<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoteResponse from(Note n) {
        return new NoteResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getLanguage(),
                List.copyOf(n.getTags()), // 영속 컬렉션과 분리한 불변 복사본
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
