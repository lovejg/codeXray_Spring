package com.codeXray.backend.note.dto;

import com.codeXray.backend.note.entity.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// PUT = 전체 교체. 생성과 동일한 필수 필드.
public record UpdateNoteRequest(
        @NotNull NoteType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String body,
        @Size(max = 30) String language,
        @Size(max = 20) List<@Size(max = 30) String> tags
) {
}
