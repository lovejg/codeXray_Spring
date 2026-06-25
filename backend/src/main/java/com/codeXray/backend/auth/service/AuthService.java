package com.codeXray.backend.auth.service;

import com.codeXray.backend.auth.dto.RegisterRequest;
import com.codeXray.backend.auth.entity.EmailVerificationToken;
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

    @Transactional
    public void register(RegisterRequest req) {
        if(userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }

        if(userRepository.existsByNickname(req.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        String encoded = passwordEncoder.encode(req.password());


        User user = User.builder()
                .email(req.email())
                .password(encoded)
                .nickname(req.nickname())
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);


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
}
