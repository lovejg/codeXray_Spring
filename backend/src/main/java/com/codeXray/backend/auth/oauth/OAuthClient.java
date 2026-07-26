package com.codeXray.backend.auth.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 소셜 OAuth 서버(구글/네이버)와의 HTTP 왕복을 담당하는 클라이언트.
 *  1) authorization code -> access token 교환
 *  2) access token -> 사용자 정보 조회
 * 서비스는 fetchGoogleUser(code) / fetchNaverUser(code, state) 하나만 호출하면
 * 정규화된 OAuthUserInfo 를 받는다.
 */
@Component
public class OAuthClient {

    private final RestClient restClient = RestClient.create();

    // ── 구글 ──
    private final String googleClientId;
    private final String googleClientSecret;
    private final String googleRedirectUri;
    private final String googleTokenUri;
    private final String googleUserInfoUri;

    // ── 네이버 ──
    private final String naverClientId;
    private final String naverClientSecret;
    private final String naverTokenUri;
    private final String naverUserInfoUri;

    public OAuthClient(
            @Value("${app.oauth.google.client-id}") String googleClientId,
            @Value("${app.oauth.google.client-secret}") String googleClientSecret,
            @Value("${app.oauth.google.redirect-uri}") String googleRedirectUri,
            @Value("${app.oauth.google.token-uri}") String googleTokenUri,
            @Value("${app.oauth.google.userinfo-uri}") String googleUserInfoUri,
            @Value("${app.oauth.naver.client-id}") String naverClientId,
            @Value("${app.oauth.naver.client-secret}") String naverClientSecret,
            @Value("${app.oauth.naver.token-uri}") String naverTokenUri,
            @Value("${app.oauth.naver.userinfo-uri}") String naverUserInfoUri) {
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.googleRedirectUri = googleRedirectUri;
        this.googleTokenUri = googleTokenUri;
        this.googleUserInfoUri = googleUserInfoUri;
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
        this.naverTokenUri = naverTokenUri;
        this.naverUserInfoUri = naverUserInfoUri;
    }

    // ── 구글 ──

    /** code 한 방으로 토큰 교환 + 유저정보 조회까지 끝내고 정규화해서 반환. */
    public OAuthUserInfo fetchGoogleUser(String code) {
        String accessToken = exchangeGoogleToken(code);
        GoogleUserInfo google = restClient.get()
                .uri(googleUserInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfo.class);
        return new OAuthUserInfo(google.sub(), google.email(), google.name());
    }

    private String exchangeGoogleToken(String code) {
        // 구글 token 엔드포인트는 form-urlencoded 로 받는다
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", googleRedirectUri);
        form.add("grant_type", "authorization_code");

        GoogleTokenResponse response = restClient.post()
                .uri(googleTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);

        return response.access_token();
    }

    // ── 네이버 ──

    /**
     * 네이버는 code 외에 state 도 토큰 교환에 필요하다(CSRF 방지 목적, authorize 때 넘긴 값 그대로).
     * 구글과 달리 redirect_uri 는 토큰 교환에 요구하지 않는다(앱 설정/authorize 에만 사용).
     */
    public OAuthUserInfo fetchNaverUser(String code, String state) {
        String accessToken = exchangeNaverToken(code, state);
        NaverMeResponse me = restClient.get()
                .uri(naverUserInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(NaverMeResponse.class);

        NaverMeResponse.NaverAccount acc = me.response();
        // 표시 이름은 nickname 우선, 없으면 name
        String displayName = (acc.nickname() != null && !acc.nickname().isBlank())
                ? acc.nickname() : acc.name();
        return new OAuthUserInfo(acc.id(), acc.email(), displayName);
    }

    private String exchangeNaverToken(String code, String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("state", state);
        form.add("client_id", naverClientId);
        form.add("client_secret", naverClientSecret);
        form.add("grant_type", "authorization_code");

        NaverTokenResponse response = restClient.post()
                .uri(naverTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(NaverTokenResponse.class);

        return response.access_token();
    }
}
