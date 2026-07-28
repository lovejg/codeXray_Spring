package com.codeXray.backend.ai.repository;

import com.codeXray.backend.ai.entity.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {
    // 소유자 확인용(남의 잡 조회 방지)
    Optional<AiJob> findByIdAndUserId(Long id, Long userId);
}
