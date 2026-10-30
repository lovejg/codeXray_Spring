package com.codeXray.backend.user.service;

import com.codeXray.backend.auth.refresh.RefreshTokenStore;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// 프로필(닉네임·비밀번호·탈퇴) 규칙. DB, Redis 없이 리포지토리와 토큰 저장소를 가짜로 두고 서비스 판단만 본다
class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
    private final UserService service = new UserService(userRepository, encoder, refreshTokenStore);

    private User given(AuthProvider provider, String rawPassword) {
        User user = User.builder()
                .email("me@test.example").nickname("알고리즘새싹")
                .password(rawPassword == null ? null : encoder.encode(rawPassword))
                .role(UserRole.USER).provider(provider).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("지금 닉네임 그대로 저장하면 중복 오류 없이 그대로 둔다")
    void sameNicknameIsNoop() {
        User user = given(AuthProvider.LOCAL, "password1");
        when(userRepository.existsByNickname("알고리즘새싹")).thenReturn(true); // 내 닉네임이니 당연히 존재

        service.updateNickname(1L, "알고리즘새싹");

        assertThat(user.getNickname()).isEqualTo("알고리즘새싹");
        verify(userRepository, never()).existsByNickname(anyString());
    }

    @Test
    @DisplayName("닉네임 앞뒤 공백은 지우고 저장한다")
    void nicknameIsTrimmed() {
        User user = given(AuthProvider.LOCAL, "password1");

        service.updateNickname(1L, "  DP장인 ");

        assertThat(user.getNickname()).isEqualTo("DP장인");
    }

    @Test
    @DisplayName("다른 사람이 쓰는 닉네임이면 거절한다")
    void duplicatedNicknameRejected() {
        given(AuthProvider.LOCAL, "password1");
        when(userRepository.existsByNickname("DP장인")).thenReturn(true);

        assertThatThrownBy(() -> service.updateNickname(1L, "DP장인"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_DUPLICATED);
    }

    @Test
    @DisplayName("소셜 계정은 비밀번호를 바꿀 수 없다는 오류를 받는다(현재 비밀번호 틀림이 아니라)")
    void socialAccountHasNoPassword() {
        given(AuthProvider.GOOGLE, null);

        assertThatThrownBy(() -> service.updatePassword(1L, "anything1", "newpassword1"))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_NO_PASSWORD);
    }

    @Test
    @DisplayName("현재 비밀번호가 맞으면 새 비밀번호로 바뀐다")
    void passwordChanged() {
        User user = given(AuthProvider.LOCAL, "password1");

        service.updatePassword(1L, "password1", "newpassword1");

        assertThat(encoder.matches("newpassword1", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호를 바꾸면 모든 기기의 로그인을 끊고, 지금 기기에 쓸 새 토큰을 발급한다")
    void passwordChangeRevokesOtherSessions() {
        given(AuthProvider.LOCAL, "password1");
        when(refreshTokenStore.issue(1L)).thenReturn("new-token");

        String token = service.updatePassword(1L, "password1", "newpassword1");

        // 순서가 거꾸로면 방금 발급한 토큰까지 지워진다
        var order = inOrder(refreshTokenStore);
        order.verify(refreshTokenStore).deleteAllOf(1L);
        order.verify(refreshTokenStore).issue(1L);
        assertThat(token).isEqualTo("new-token");
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 거절하고, 로그인도 끊지 않는다")
    void wrongCurrentPassword() {
        given(AuthProvider.LOCAL, "password1");

        assertThatThrownBy(() -> service.updatePassword(1L, "wrong-password", "newpassword1"))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        verifyNoInteractions(refreshTokenStore);
    }

    @Test
    @DisplayName("탈퇴하면 그 유저를 지우고 모든 기기의 로그인 토큰도 지운다")
    void deleteAccount() {
        User user = given(AuthProvider.LOCAL, "password1");

        service.deleteAccount(1L);

        verify(userRepository).delete(user);
        verify(refreshTokenStore).deleteAllOf(1L);
    }
}
