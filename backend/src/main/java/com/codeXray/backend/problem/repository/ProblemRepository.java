package com.codeXray.backend.problem.repository;

import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.entity.Tier;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository
        extends JpaRepository<Problem, Long>, ProblemRepositoryCustom {

    // 체감 난이도 재계산용: 문제 행에 쓰기 잠금(SELECT ... FOR UPDATE)을 걸고 읽는다.
    // 같은 문제에 피드백이 동시에 들어와도 재계산이 한 줄로 서서 마지막 결과가 모든 피드백을 반영한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Problem p where p.id = :id")
    Optional<Problem> findByIdForUpdate(@Param("id") Long id);

    // 붙여넣은 URL 매칭용: link 가 "/lessons/{번호}" 로 끝나는 문제 조회
    // (파생 쿼리 → 자동으로 WHERE link LIKE %/lessons/{번호} 생성)
    Optional<Problem> findByLinkEndingWith(String suffix);

    // 티어가 아직 계산되지 않은 문제가 하나라도 있는지 (앱 시작 시 재계산 여부 판단, TierBootstrap)
    // (파생 쿼리 → WHERE tier IS NULL 을 하나 찾으면 멈춤)
    boolean existsByTierIsNull();

    // AI 힌트용: 문제의 알고리즘 태그 이름들 (lazy 컬렉션 대신 스칼라 조회)
    @Query("select t.name from Problem p join p.problemTags pt join pt.tag t where p.id = :id")
    List<String> findTagNamesByProblemId(@Param("id") Long id);

    // ── 추천용 ──
    // 약점 태그를 가진 미해결 문제 (정답률 높은=접근 쉬운 순)
    @Query("select distinct p from Problem p join p.problemTags pt join pt.tag t " +
            "where t.name in :tags and p.id not in :excludeIds " +
            "order by p.acceptanceRate desc nulls last")
    List<Problem> recommendByTags(@Param("tags") List<String> tags,
                                  @Param("excludeIds") List<Long> excludeIds,
                                  Pageable pageable);

    // 적정 티어 구간의 미해결 문제 (정답률 높은 순)
    @Query("select p from Problem p where p.tier in :tiers and p.id not in :excludeIds " +
            "order by p.acceptanceRate desc nulls last")
    List<Problem> recommendByTiers(@Param("tiers") List<Tier> tiers,
                                   @Param("excludeIds") List<Long> excludeIds,
                                   Pageable pageable);
}
