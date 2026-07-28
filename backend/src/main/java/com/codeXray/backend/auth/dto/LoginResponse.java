package com.codeXray.backend.auth.dto;

// accessToken 은 항상. refreshToken 은 확장(비브라우저 클라이언트)에만 body 로 내려줌.
// 웹은 refreshToken 을 null 로 두고 httpOnly 쿠키로만 받는다.
public record LoginResponse(String accessToken, String refreshToken) {

    // 웹용: refresh 는 쿠키로 나가므로 body 엔 accessToken 만
    public static LoginResponse web(String accessToken) {
        return new LoginResponse(accessToken, null);
    }

    // 확장용: 쿠키를 못 쓰므로 refresh 도 body 로
    public static LoginResponse full(String accessToken, String refreshToken) {
        return new LoginResponse(accessToken, refreshToken);
    }
}
