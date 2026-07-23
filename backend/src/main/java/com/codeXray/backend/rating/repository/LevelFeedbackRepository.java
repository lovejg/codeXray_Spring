package com.codeXray.backend.rating.repository;

import com.codeXray.backend.rating.entity.LevelFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LevelFeedbackRepository extends JpaRepository<LevelFeedback, Long> {

    // 내 피드백 조회용(upsert)
    Optional<LevelFeedback> findByUserIdAndProblemId(Long userId, Long problemId);

    // 특정 문제 재계산 시 그 문제에 달린 모든 피드백 레벨 수집
    List<LevelFeedback> findByProblemId(Long problemId);
}
