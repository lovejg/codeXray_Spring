package com.codeXray.backend.user.repository;

import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    // 소셜 로그인: (프로바이더 + 프로바이더가 준 고유 ID) 조합으로 기존 유저 찾기
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Boolean existsByEmail(String email);

    Boolean existsByNickname(String nickname);

    void deleteById(Long id);

    // 관리자 전체에게 알림 broadcast 할 때, id만 가볍게 조회
    @Query("select u.id from User u where u.role = :role")
    List<Long> findIdsByRole(UserRole role);
}
