package com.codeXray.backend.auth.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 구글 OAuth 서버와의 HTTP 왕복을 담당하는 클라이언트.
 *  1) authorization code -> access token 교환  (exchangeCodeForToken)
 *  2) access token -> 사용자 정보 조회            (fetchUserInfo)
 * 서비스는 fetchGoogleUser(code) 하나만 호출하면 정규화된 OAuthUserInfo를 받는다.
 */
@Component
public class OAuthClient {

    private final RestClient restClient = RestClient.create();

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenUri;
    private final String userInfoUri;

    public OAuthClient(
            @Value("${app.oauth.google.client-id}") String clientId,
            @Value("${app.oauth.google.client-secret}") String clientSecret,
            @Value("${app.oauth.google.redirect-uri}") String redirectUri,
            @Value("${app.oauth.google.token-uri}") String tokenUri,
            @Value("${app.oauth.google.userinfo-uri}") String userInfoUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
    }

    /** code 한 방으로 토큰 교환 + 유저정보 조회까지 끝내고 정규화해서 반환. */
    public OAuthUserInfo fetchGoogleUser(String code) {
        String accessToken = exchangeCodeForToken(code);
        GoogleUserInfo google = fetchUserInfo(accessToken);
        return new OAuthUserInfo(google.sub(), google.email(), google.name());
    }

    private String exchangeCodeForToken(String code) {
        // 구글 token 엔드포인트는 form-urlencoded 로 받는다
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        GoogleTokenResponse response = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);

        return response.access_token();
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) {
        return restClient.get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfo.class);
    }
}
