package com.codeXray.backend.user.dto;

import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;

public record UserResponse(Long id, String email, String nickname, UserRole role, boolean emailVerified) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.isEmailVerified()
        );
    }
}
