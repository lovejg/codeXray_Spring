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
        OAuthUserInfo info = oAuthClient.fetchGoogleUser(code);

        // (provider=GOOGLE, providerId)로 기존 유저 찾고, 없으면 새로 가입
        User user = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, info.providerId())
                .orElseGet(() -> register(info));

        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getRole());
        String refreshToken = refreshTokenStore.issue(user.getId());
        return new TokenPair(accessToken, refreshToken);
    }

    private User register(OAuthUserInfo info) {
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
                .provider(AuthProvider.GOOGLE)
                .providerId(info.providerId())
                .build();

        // 구글이 이미 이메일을 검증했으므로 인증완료 상태로 시작(도메인 메서드 재사용)
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
