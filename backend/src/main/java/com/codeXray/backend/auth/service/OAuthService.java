package com.codeXray.backend.auth.service;

import com.codeXray.backend.auth.jwt.JwtUtil;
import com.codeXray.backend.auth.jwt.TokenPair;
import com.codeXray.backend.auth.oauth.OAuthClient;
import com.codeXray.backend.auth.oauth.OAuthUserInfo;
import com.codeXray.backend.auth.refresh.RefreshTokenStore;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.UUID;


@Service
public class OAuthService {

    private final OAuthClient oAuthClient;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;
    private final String adminEmail;

    public OAuthService(OAuthClient oAuthClient,
                        UserRepository userRepository,
                        JwtUtil jwtUtil,
                        RefreshTokenStore refreshTokenStore,
                        @Value("${app.admin.email}") String adminEmail) {
        this.oAuthClient = oAuthClient;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.refreshTokenStore = refreshTokenStore;
        this.adminEmail = adminEmail;
    }

    @Transactional
    public TokenPair loginWithGoogle(String code) {
        // 구글과 통신 (토큰 교환 + 유저정보 조회) — 세부는 OAuthClient가 감춰줌
        OAuthUserInfo info = fetchOrFail(() -> oAuthClient.fetchGoogleUser(code));
        return loginOrRegister(info, AuthProvider.GOOGLE);
    }

    @Transactional
    public TokenPair loginWithNaver(String code, String state) {
        // 네이버는 토큰 교환에 state가 필요 (authorize 때 넘긴 값)
        OAuthUserInfo info = fetchOrFail(() -> oAuthClient.fetchNaverUser(code, state));
        return loginOrRegister(info, AuthProvider.NAVER);
    }

    /**
     * 소셜 서버와의 통신(토큰교환/유저조회)이 실패하면 날것의 500 대신 OAUTH_FAILED(401)로 변환.
     * (잘못된 code, 만료, 자격증명 오류 등은 모두 "소셜 로그인 실패"로 사용자에게 보임)
     */
    private OAuthUserInfo fetchOrFail(java.util.function.Supplier<OAuthUserInfo> fetch) {
        try {
            return fetch.get();
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    /** (provider, providerId)로 기존 유저 찾고 없으면 가입 → 우리 토큰쌍 발급. 구글/네이버 공통. */
    private TokenPair loginOrRegister(OAuthUserInfo info, AuthProvider provider) {
        User user = userRepository.findByProviderAndProviderId(provider, info.providerId())
                .orElseGet(() -> register(info, provider));

        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getRole());
        String refreshToken = refreshTokenStore.issue(user.getId());
        return new TokenPair(accessToken, refreshToken);
    }

    private User register(OAuthUserInfo info, AuthProvider provider) {
        // 소셜 계정이 이메일 제공에 동의하지 않으면 email이 null일 수 있음 → 가입 불가
        if (info.email() == null || info.email().isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_REQUIRED);
        }
        // 같은 이메일이 이미 local 가입 등으로 존재하면 그냥 만들 수 없음
        // 가장 단순한 해결책은 충돌 에러(나중에 계정 연결로 발전시킬 수 있음)
        if (userRepository.existsByEmail(info.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }

        UserRole role = info.email().equals(adminEmail) ? UserRole.ADMIN : UserRole.USER;

        User user = User.builder()
                .email(info.email())
                .nickname(resolveNickname(info))
                .role(role)
                .provider(provider)
                .providerId(info.providerId())
                .build();

        // 소셜 프로바이더가 이미 이메일을 검증했으므로 인증완료 상태로 시작(도메인 메서드 재사용)
        user.verifyEmail();

        return userRepository.save(user);
    }

    private String resolveNickname(OAuthUserInfo info) {
        String base = (info.name() != null && !info.name().isBlank()) ? info.name() : "user";
        String nickname = base;
        while (userRepository.existsByNickname(nickname)) {
            nickname = base + "_" + UUID.randomUUID().toString().substring(0, 4);
        }
        return nickname;
    }
}
