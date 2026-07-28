package com.codeXray.backend.solution.repository;

import com.codeXray.backend.problem.entity.Tier;
import com.codeXray.backend.solution.entity.Solution;
import com.codeXray.backend.stats.dto.StatsProjections.LangCount;
import com.codeXray.backend.stats.dto.StatsProjections.TagCount;
import com.codeXray.backend.stats.dto.StatsProjections.TierCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    // 풀이 이미 있는지 확인하는 용(upsert)
    Optional<Solution> findByUserIdAndProblemId(Long userId, Long problemId);

    // 내 풀이 목록(최신순)
    List<Solution> findByUserIdOrderByUpdatedAtDesc(Long userId);

    // 내 풀이 목록 중 starred 필터
    List<Solution> findByUserIdAndStarredOrderByUpdatedAtDesc(Long userId, boolean starred);

    // ── 대시보드 통계용 ──
    long countByUserId(Long userId);
    long countByUserIdAndStarred(Long userId, boolean starred);

    // 티어별 해결 수
    @Query("select s.problem.tier as tier, count(s) as count from Solution s " +
            "where s.userId = :userId group by s.problem.tier")
    List<TierCount> tierDistribution(@Param("userId") Long userId);

    // 언어별 해결 수(많은 순)
    @Query("select s.language as language, count(s) as count from Solution s " +
            "where s.userId = :userId group by s.language order by count(s) desc")
    List<LangCount> languageDistribution(@Param("userId") Long userId);

    // 많이 푼 태그(강점) — Pageable 로 상위 N
    @Query("select t.name as tag, count(s) as count from Solution s " +
            "join s.problem p join p.problemTags pt join pt.tag t " +
            "where s.userId = :userId group by t.name order by count(s) desc")
    List<TagCount> topTags(@Param("userId") Long userId, Pageable pageable);

    // 스트릭/잔디밭 계산용: 풀이 생성 시각 전부 (일자 집계는 Java 에서)
    @Query("select s.createdAt from Solution s where s.userId = :userId")
    List<LocalDateTime> findCreatedAtsByUserId(@Param("userId") Long userId);

    // ── 추천용 ──
    @Query("select s.problem.id from Solution s where s.userId = :userId")
    List<Long> findSolvedProblemIds(@Param("userId") Long userId);

    @Query("select s.problem.tier from Solution s where s.userId = :userId and s.problem.tier is not null")
    List<Tier> findSolvedTiers(@Param("userId") Long userId);

    // TIER_UP 판정: 이 문제를 제외하고, 같은 티어 패밀리 문제를 이미 푼 적이 있는지 카운트
    @Query("select count(s) from Solution s " +
            "where s.userId = :userId and s.problem.id <> :problemId and s.problem.tier in :tiers")
    long countByUserInTiers(@Param("userId") Long userId,
                            @Param("problemId") Long problemId,
                            @Param("tiers") List<Tier> tiers);
}
