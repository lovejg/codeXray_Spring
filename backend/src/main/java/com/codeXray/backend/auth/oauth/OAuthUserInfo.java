package com.codeXray.backend.auth.oauth;

/**
 * 프로바이더(구글/카카오/...)에 상관없이 정규화한 사용자 정보.
 * OAuthClient가 구글 응답을 이 모양으로 변환해서 서비스에 넘긴다.
 */
public record OAuthUserInfo(String providerId, String email, String name) {
}
