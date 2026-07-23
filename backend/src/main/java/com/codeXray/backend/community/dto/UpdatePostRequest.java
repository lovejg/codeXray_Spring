package com.codeXray.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 작성자 수정. isPrivate 는 선택(null 이면 기존값 유지).
public record UpdatePostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String content,
        Boolean isPrivate
) {
}
