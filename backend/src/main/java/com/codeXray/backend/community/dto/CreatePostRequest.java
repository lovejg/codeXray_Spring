package com.codeXray.backend.community.dto;

import com.codeXray.backend.community.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        Long problemId,               // 선택 (문제 연결)
        @NotNull PostType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String content,
        Boolean isPrivate             // 선택 (기본 false)
) {
}
