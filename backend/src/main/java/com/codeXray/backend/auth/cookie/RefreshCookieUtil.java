package com.codeXray.backend.auth.cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/* Refresh Token을 쿠키로 굽고 지우고 하는 쿠키 유틸 클래스 */

@Component
public class RefreshCookieUtil {

    public static final String COOKIE_NAME = "refreshToken";

    private final boolean secure;
    private final long maxAgeDays;

    public RefreshCookieUtil(
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.jwt.refresh-expiration-days}") long maxAgeDays
    ) {
        this.secure = secure;
        this.maxAgeDays = maxAgeDays;
    }

    public ResponseCookie create(String token) {
        return baseBuilder(token)
                .maxAge(Duration.ofDays(maxAgeDays))
                .build();
    }

    public ResponseCookie clear() {
        return baseBuilder("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax");
    }
}
