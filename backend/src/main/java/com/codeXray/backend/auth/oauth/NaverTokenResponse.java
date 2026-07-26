package com.codeXray.backend.auth.oauth;

/**
 * 네이버 토큰 교환 응답 중 우리가 쓰는 필드만.
 * 필드명을 JSON 키(access_token)와 그대로 맞춘 이유는 GoogleTokenResponse와 동일:
 * Boot 4에서는 @JsonProperty가 컴파일 클래스패스에 없어 이름 매핑에 의존한다.
 * (나머지 refresh_token/token_type/expires_in 등은 무시됨)
 */
record NaverTokenResponse(String access_token) {
}
