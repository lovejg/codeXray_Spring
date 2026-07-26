package com.codeXray.backend.auth.oauth;

/**
 * 네이버 userinfo(GET /v1/nid/me) 응답.
 * 실제 사용자 정보는 최상위가 아니라 중첩된 "response" 객체 안에 들어온다:
 *   { "resultcode": "00", "message": "success", "response": { "id":..., "email":..., ... } }
 * 그래서 필드명을 JSON 키 그대로 response 로 두고, 안쪽을 NaverAccount 로 받는다.
 */
record NaverMeResponse(NaverAccount response) {

    record NaverAccount(String id, String email, String nickname, String name) {
    }
}
