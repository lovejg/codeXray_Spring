package com.codeXray.backend.auth.oauth;

/**
 * 구글 userinfo 응답 중 우리가 쓰는 필드만.
 * sub = 구글이 부여한 고유 사용자 ID (우리 providerId로 저장), email, name(표시 이름).
 */
record GoogleUserInfo(String sub, String email, String name) {
}
