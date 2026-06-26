package com.codeXray.backend.auth.controller;

import com.codeXray.backend.auth.cookie.RefreshCookieUtil;
import com.codeXray.backend.auth.dto.*;
import com.codeXray.backend.auth.jwt.TokenPair;
import com.codeXray.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieUtil refreshCookieUtil;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req.email(), req.password(), req.nickname());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req.token());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        TokenPair tokenPair = authService.login(req.email(), req.password());
        ResponseCookie cookie = refreshCookieUtil.create(tokenPair.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString()); // 쿠키를 실을 response 객체
        return ResponseEntity.ok(new LoginResponse(tokenPair.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@CookieValue(name = RefreshCookieUtil.COOKIE_NAME) String refreshToken,
                                                 HttpServletResponse response) {
        TokenPair tokenPair = authService.refresh(refreshToken);
        ResponseCookie cookie = refreshCookieUtil.create(tokenPair.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(new LoginResponse(tokenPair.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = RefreshCookieUtil.COOKIE_NAME, required = false) String refreshToken,
                                       HttpServletResponse response) {
        if(refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie cookie = refreshCookieUtil.clear();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        authService.resendVerification(req.email());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
