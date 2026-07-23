package com.codeXray.backend.note.dto;

import com.codeXray.backend.note.entity.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateNoteRequest(
        @NotNull NoteType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String body,
        @Size(max = 30) String language,
        // 최대 20개, 각 태그 최대 30자 (원소 검증은 List<@Size ...>)
        @Size(max = 20) List<@Size(max = 30) String> tags
) {
}
