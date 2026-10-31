package com.codeXray.backend.auth.service;

import com.codeXray.backend.auth.entity.EmailVerificationToken;
import com.codeXray.backend.auth.jwt.JwtUtil;
import com.codeXray.backend.auth.jwt.TokenPair;
import com.codeXray.backend.auth.refresh.RefreshTokenStore;
import com.codeXray.backend.auth.repository.EmailVerificationTokenRepository;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.mail.MailService;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final MailService mailService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final StringRedisTemplate redisTemplate;


    @Transactional
    public void register(String email, String password, String nickname) {
        if(userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }

        if(userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        String encoded = passwordEncoder.encode(password);

        User user = User.builder()
                .email(email)
                .password(encoded)
                .nickname(nickname)
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);

        issueAndSendVerification(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken emailVerificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if(emailVerificationToken.getUsedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if(emailVerificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        emailVerificationToken.markAsUsed();

        User user = userRepository.findById(emailVerificationToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        user.verifyEmail();
    }

    /*
     * 로그인.
     *  - 같은 이메일로 15분 안에 5번 틀리면 잠깐 막는다(LoginAttemptLimiter)
     *  - 없는 이메일이어도 비밀번호 해시 비교를 한 번 한다. 안 하면 "없는 이메일은 즉시 실패, 있는 이메일은 bcrypt 만큼 늦게 실패"라
     *    응답 시간만 재도 가입 여부를 알 수 있다
     */
    @Transactional(readOnly = true)
    public TokenPair login(String email, String password) {
        loginAttemptLimiter.checkAllowed(email);

        User user = userRepository.findByEmail(email).orElse(null);
        String hash = (user != null && user.getPassword() != null) ? user.getPassword() : dummyHash();
        boolean matches = passwordEncoder.matches(password, hash);

        if (user == null || user.getPassword() == null || !matches) {
            loginAttemptLimiter.recordFailure(email);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        loginAttemptLimiter.reset(email);

        if(!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getRole());
        String refreshToken = refreshTokenStore.issue(user.getId());

        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public TokenPair refresh(String refreshToken) {
        // 꺼내면서 지운다(원자적). 같은 토큰으로 동시에 와도 하나만 통과한다 — rotation의 핵심
        Long userId = refreshTokenStore.consume(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 탈퇴한 계정의 토큰이 다른 기기에 남아 있던 경우. 404 가 아니라 "다시 로그인"으로 끝낸다
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        String newAccessToken = jwtUtil.createAccessToken(userId, user.getRole());
        String newRefreshToken = refreshTokenStore.issue(userId);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenStore.delete(refreshToken);
    }

    /*
     * 인증 메일 재발송. 결과를 말해 주지 않는다(항상 성공처럼 끝남).
     * 예전에는 없는 이메일 → 404, 이미 인증 → 409 를 돌려줘서 이 API 로 가입 여부를 하나씩 확인할 수 있었고,
     * 횟수 제한도 없어 남의 메일함에 인증 메일을 계속 보낼 수 있었다. 이제 이메일마다 1분에 한 번만 보낸다
     */
    @Transactional
    public void resendVerification(String email) {
        String cooldownKey = RESEND_COOLDOWN_PREFIX + email.trim().toLowerCase(java.util.Locale.ROOT);
        Boolean first = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(first)) return;

        userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueAndSendVerification);
    }


    /* -------------- 헬퍼 -------------- */
    private static final String RESEND_COOLDOWN_PREFIX = "verify:resend:";
    private static final java.time.Duration RESEND_COOLDOWN = java.time.Duration.ofMinutes(1);

    // 없는 이메일로 로그인할 때 비교에 쓰는 가짜 해시. 처음 필요할 때 한 번 만든다
    private volatile String dummyHash;

    private String dummyHash() {
        String h = dummyHash;
        if (h == null) {
            h = passwordEncoder.encode(UUID.randomUUID().toString());
            dummyHash = h;
        }
        return h;
    }

    private void issueAndSendVerification(User user) {
        // 새 토큰 발급 전 기존 토큰 제거 → 재발송 시 이전 링크는 무효(INVALID_TOKEN)
        emailVerificationTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        EmailVerificationToken emailVerificationToken = EmailVerificationToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(expiresAt)
                .build();

        emailVerificationTokenRepository.save(emailVerificationToken);

        mailService.sendVerificationEmail(user.getEmail(), token);
    }
}
