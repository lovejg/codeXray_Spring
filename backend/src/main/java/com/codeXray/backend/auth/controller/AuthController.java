package com.codeXray.backend.auth.controller;

import com.codeXray.backend.auth.cookie.RefreshCookieUtil;
import com.codeXray.backend.auth.dto.*;
import com.codeXray.backend.auth.jwt.TokenPair;
import com.codeXray.backend.auth.service.AuthService;
import com.codeXray.backend.auth.service.OAuthService;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
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
    private final OAuthService oAuthService;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req,
                                               @RequestHeader(value = "X-Client", required = false) String client,
                                               HttpServletResponse response) {
        TokenPair tokenPair = authService.login(req.email(), req.password());
        // 확장(비브라우저)은 쿠키를 못 쓰므로 refresh 도 body 로 내려줌 (웹은 이 헤더를 안 보냄)
        if ("extension".equals(client)) {
            return ResponseEntity.ok(LoginResponse.full(tokenPair.accessToken(), tokenPair.refreshToken()));
        }
        ResponseCookie cookie = refreshCookieUtil.create(tokenPair.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString()); // 쿠키를 실을 response 객체
        return ResponseEntity.ok(LoginResponse.web(tokenPair.accessToken()));
    }

    // refresh 토큰은 웹=쿠키, 확장=X-Refresh-Token 헤더 로 받는다.
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@CookieValue(name = RefreshCookieUtil.COOKIE_NAME, required = false) String cookieToken,
                                                 @RequestHeader(value = "X-Refresh-Token", required = false) String headerToken,
                                                 HttpServletResponse response) {
        boolean isExtension = headerToken != null;
        String provided = isExtension ? headerToken : cookieToken;
        if (provided == null) throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);

        TokenPair tokenPair = authService.refresh(provided);
        if (isExtension) {
            // 회전된 새 refresh 를 확장이 저장하도록 body 로 반환
            return ResponseEntity.ok(LoginResponse.full(tokenPair.accessToken(), tokenPair.refreshToken()));
        }
        ResponseCookie cookie = refreshCookieUtil.create(tokenPair.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(LoginResponse.web(tokenPair.accessToken()));
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

    @PostMapping("/oauth/google")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody OAuthLoginRequest req, HttpServletResponse response) {
        TokenPair tokenPair = oAuthService.loginWithGoogle(req.code());
        ResponseCookie cookie = refreshCookieUtil.create(tokenPair.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(LoginResponse.web(tokenPair.accessToken()));
    }

    @PostMapping("/oauth/naver")
    public ResponseEntity<LoginResponse> naverLogin(@Valid @RequestBody NaverLoginRequest req, HttpServletResponse response) {
        TokenPair tokenPair = oAuthService.loginWithNaver(req.code(), req.state());
        ResponseCookie cookie = refreshCookieUtil.create(tokenPair.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(LoginResponse.web(tokenPair.accessToken()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        authService.resendVerification(req.email());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
