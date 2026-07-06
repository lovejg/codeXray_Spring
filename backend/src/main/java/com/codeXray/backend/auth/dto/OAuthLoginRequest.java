package com.codeXray.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

// 프론트가 구글에서 받아온 authorization code를 담아 보낸다
public record OAuthLoginRequest(@NotBlank String code) {
}
