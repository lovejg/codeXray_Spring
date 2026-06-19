package com.codeXray.backend.auth.service;

import com.codeXray.backend.auth.dto.RegisterRequest;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        // (다음 단계) 이메일 인증 토큰 생성 + 메일 발송
    }
}
