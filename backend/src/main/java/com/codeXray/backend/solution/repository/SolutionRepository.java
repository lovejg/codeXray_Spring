package com.codeXray.backend.solution.repository;

import com.codeXray.backend.problem.entity.Tier;
import com.codeXray.backend.solution.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    // 풀이 이미 있는지 확인하는 용(upsert)
    Optional<Solution> findByUserIdAndProblemId(Long userId, Long problemId);

    // 내 풀이 목록(최신순)
    List<Solution> findByUserIdOrderByUpdatedAtDesc(Long userId);

    // 내 풀이 목록 중 starred 필터
    List<Solution> findByUserIdAndStarredOrderByUpdatedAtDesc(Long userId, boolean starred);

    // TIER_UP 판정: 이 문제를 제외하고, 같은 티어 패밀리 문제를 이미 푼 적이 있는지 카운트
    @Query("select count(s) from Solution s " +
            "where s.userId = :userId and s.problem.id <> :problemId and s.problem.tier in :tiers")
    long countByUserInTiers(@Param("userId") Long userId,
                            @Param("problemId") Long problemId,
                            @Param("tiers") List<Tier> tiers);
}
