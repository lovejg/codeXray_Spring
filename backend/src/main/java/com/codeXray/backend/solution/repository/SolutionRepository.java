package com.codeXray.backend.solution.repository;

import com.codeXray.backend.solution.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    // 풀이 이미 있는지 확인하는 용(upsert)
    Optional<Solution> findByUserIdAndProblemId(Long userId, Long problemId);

    // 내 풀이 목록(최신순)
    List<Solution> findByUserIdOrderByUpdatedAtDesc(Long userId);

    // 내 풀이 목록 중 starred 필터
    List<Solution> findByUserIdAndStarredOrderByUpdatedAtDesc(Long userId, boolean starred);
}
