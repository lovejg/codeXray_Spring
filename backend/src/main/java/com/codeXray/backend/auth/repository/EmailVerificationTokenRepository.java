package com.codeXray.backend.auth.repository;

import com.codeXray.backend.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);

    // 새 인증 메일 발급 전, 그 유저의 기존(미사용) 토큰을 모두 제거 → 이전 링크 무효화
    @Modifying
    @Query("delete from EmailVerificationToken t where t.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 오래 전에 만료됐거나 사용된 토큰 정리(스케줄러). 삭제 건수 반환.
    @Modifying
    @Query("delete from EmailVerificationToken t where t.expiresAt < :cutoff or t.usedAt < :cutoff")
    int deleteStale(@Param("cutoff") LocalDateTime cutoff);
}
