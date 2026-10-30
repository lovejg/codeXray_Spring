package com.codeXray.backend.user.controller;

import com.codeXray.backend.auth.cookie.RefreshCookieUtil;
import com.codeXray.backend.user.dto.UpdateNicknameRequest;
import com.codeXray.backend.user.dto.UpdatePasswordRequest;
import com.codeXray.backend.user.dto.UserResponse;
import com.codeXray.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RefreshCookieUtil refreshCookieUtil;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Long userId) {
        UserResponse body = userService.getMe(userId);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(@AuthenticationPrincipal Long userId,
                                               @RequestBody @Valid UpdateNicknameRequest req) {
        userService.updateNickname(userId, req.nickname());
        return ResponseEntity.ok().build();
    }

    // 다른 기기는 로그아웃되고, 지금 기기는 새 refresh 쿠키로 로그인이 이어진다
    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal Long userId,
                                               @RequestBody @Valid UpdatePasswordRequest req) {
        String refreshToken = userService.updatePassword(userId, req.currentPassword(), req.newPassword());
        ResponseCookie cookie = refreshCookieUtil.create(refreshToken);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Long userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
