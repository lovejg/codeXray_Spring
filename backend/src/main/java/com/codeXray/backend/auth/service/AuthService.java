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

    @Transactional(readOnly = true)
    public TokenPair login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if(!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if(!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }


        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getRole());
        String refreshToken = refreshTokenStore.issue(user.getId());

        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public TokenPair refresh(String refreshToken) {
        Long userId = refreshTokenStore.findUserId(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        refreshTokenStore.delete(refreshToken); // rotation의 핵심

        String newAccessToken = jwtUtil.createAccessToken(userId, user.getRole());
        String newRefreshToken = refreshTokenStore.issue(userId);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenStore.delete(refreshToken);
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if(user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ALREADY_VERIFIED);
        }

        issueAndSendVerification(user);
    }


    /* -------------- 헬퍼 -------------- */
    private void issueAndSendVerification(User user) {
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
