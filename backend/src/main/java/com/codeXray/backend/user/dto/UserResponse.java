package com.codeXray.backend.user.dto;

import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;

// provider: 화면이 소셜 계정에는 비밀번호 변경을 숨기는 데 쓴다
public record UserResponse(Long id, String email, String nickname, UserRole role, boolean emailVerified,
                           AuthProvider provider) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.isEmailVerified(),
                user.getProvider()
        );
    }
}
