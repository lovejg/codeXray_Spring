package com.codeXray.backend.auth.oauth;

/**
 * 구글 토큰 교환(POST /token) 응답 중 우리가 쓰는 필드만.
 * <p>
 * 필드명이 관례(accessToken)가 아니라 access_token인 이유:
 * Boot 4에서는 Jackson 어노테이션(@JsonProperty)이 컴파일 클래스패스에 없어서,
 * JSON 키와 record 컴포넌트 이름을 그대로 맞춰 매핑되게 했다.
 * (응답의 나머지 필드 token_type/expires_in/id_token 등은 알아서 무시됨)
 */
record GoogleTokenResponse(String access_token) {
}
