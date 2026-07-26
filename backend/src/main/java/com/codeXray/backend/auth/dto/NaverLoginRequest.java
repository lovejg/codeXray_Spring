package com.codeXray.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

// 프론트가 네이버에서 받아온 authorization code + state(CSRF 검증용)를 담아 보낸다
public record NaverLoginRequest(@NotBlank String code, @NotBlank String state) {
}
