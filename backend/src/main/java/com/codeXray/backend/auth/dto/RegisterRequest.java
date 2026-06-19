package com.codeXray.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        // TODO: password — 빈 값 금지(@NotBlank) + 최소 8자(@Size(min = 8, message = ...))
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이여야 합니다.")
        String password,

        // TODO: nickname — 빈 값 금지 + 2~20자(@Size(min = 2, max = 20, ...))
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2자에서 20자 사이여야 합니다.")
        String nickname
) {}
