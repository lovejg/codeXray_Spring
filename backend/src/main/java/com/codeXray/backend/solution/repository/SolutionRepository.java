package com.codeXray.backend.solution.repository;

import com.codeXray.backend.problem.entity.Tier;
import com.codeXray.backend.solution.entity.Solution;
import com.codeXray.backend.stats.dto.StatsProjections.LangCount;
import com.codeXray.backend.stats.dto.StatsProjections.TagCount;
import com.codeXray.backend.stats.dto.StatsProjections.TierCount;
import org.springframework.data.domain.Page;
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

    /*
     * 내 풀이 목록(최신순, 페이지).
     * 응답이 풀이마다 문제(제목·태그)와 메모를 싣기 때문에 둘을 한 쿼리로 같이 가져온다(join fetch).
     * 예전 파생 쿼리(findByUserIdOrderByUpdatedAtDesc)는 풀이만 읽어서, 풀이 N건에 대해
     *   - 문제 N번 (Solution.problem 지연 로딩, Problem 은 클래스 레벨 @BatchSize 가 없어 한 건씩)
     *   - 메모 N번 (역방향 @OneToOne 은 프록시를 못 만들어 LAZY 가 무시되고 무조건 조회)
     *   - 문제 태그 N번 (문제가 한 건씩 풀리니 problemTags 의 @BatchSize 도 묶을 게 없음)
     * 이 더해져 20건에 select 72개였다. 지금은 내용 1 + count 1 + 태그 배치 1 + 태그 이름 배치 1 = 4쿼리로 고정
     * (SolutionListQueryCountTest). to-one 만 fetch join 하므로 SQL limit 그대로 페이지가 잘린다
     */
    @Query(value = "select s from Solution s join fetch s.problem left join fetch s.memo " +
            "where s.userId = :userId order by s.updatedAt desc, s.id desc",
            countQuery = "select count(s) from Solution s where s.userId = :userId")
    Page<Solution> findPageByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "select s from Solution s join fetch s.problem left join fetch s.memo " +
            "where s.userId = :userId and s.starred = :starred order by s.updatedAt desc, s.id desc",
            countQuery = "select count(s) from Solution s where s.userId = :userId and s.starred = :starred")
    Page<Solution> findPageByUserIdAndStarred(@Param("userId") Long userId,
                                              @Param("starred") boolean starred,
                                              Pageable pageable);

    // 문제 상세 화면용: 이 문제에 대한 내 풀이(있으면). 예전 화면은 이것 하나를 알려고 내 풀이 전체를 받았다
    @Query("select s from Solution s join fetch s.problem left join fetch s.memo " +
            "where s.userId = :userId and s.problem.id = :problemId")
    Optional<Solution> findDetailedByUserIdAndProblemId(@Param("userId") Long userId,
                                                        @Param("problemId") Long problemId);

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
