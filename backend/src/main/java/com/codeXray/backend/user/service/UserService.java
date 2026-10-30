package com.codeXray.backend.user.service;

import com.codeXray.backend.auth.refresh.RefreshTokenStore;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.user.dto.UserResponse;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        User user = getUser(userId);
        String trimmed = nickname.trim();

        // 지금 닉네임 그대로면 바꿀 게 없다. 중복 검사를 하면 내 닉네임과 부딪혀 "이미 사용 중"이 나온다
        if (trimmed.equals(user.getNickname())) return;

        if (userRepository.existsByNickname(trimmed)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        user.changeNickname(trimmed);
    }

    // 다른 기기의 로그인을 모두 끊고, 지금 기기에 쓸 새 refresh 토큰을 돌려준다.
    // 이미 발급된 access 토큰은 만료(30분)까지 남는다
    @Transactional
    public String updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);

        // 소셜 가입 계정은 비밀번호 칸이 비어 있다. 그대로 비교하면 "현재 비밀번호가 틀림"으로 보여 헷갈린다
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_NO_PASSWORD);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        refreshTokenStore.deleteAllOf(userId);
        return refreshTokenStore.issue(userId);
    }

    // 풀이·노트·알림 등은 DB 의 ON DELETE CASCADE 로, 글·댓글은 SET NULL(작성자만 비움)로 정리된다.
    // 로그인 토큰은 DB 밖(Redis)이라 따로 지운다
    @Transactional
    public void deleteAccount(Long userId) {
        userRepository.delete(getUser(userId));
        refreshTokenStore.deleteAllOf(userId);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
