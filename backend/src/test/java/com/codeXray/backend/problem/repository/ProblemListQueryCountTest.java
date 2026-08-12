package com.codeXray.backend.problem.repository;

import com.codeXray.backend.config.QueryDslConfig;
import com.codeXray.backend.problem.dto.ProblemResponse;
import com.codeXray.backend.problem.entity.Problem;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 문제 목록 조회의 select 쿼리 수를 고정한다.
 *
 * ── 이 테스트가 지키는 범위 ──
 * 지키는 것 : ProblemRepository.search() 호출 + ProblemResponse.from() 매핑 구간에서
 *            나가는 select 쿼리 수. 즉 "리포지토리 조회 + DTO 매핑" 구간뿐이다.
 * 지키지 않는 것 : 엔드포인트 전체(GET /api/problems)가 아니다. ProblemService.search() 에
 *            걸린 @Cacheable(Redis) 캐시 구간은 이 테스트가 전혀 커버하지 않는다.
 *            서비스를 거치면 캐시 히트 시 쿼리가 0건이 나와 측정 자체가 무의미해지므로
 *            의도적으로 리포지토리를 직접 호출한다.
 *
 * ── 왜 이 값을 고정하는가 ──
 * ProblemTag → AlgorithmTag 계층은 원래 N+1 이 터지던 곳이고, 두 개의 @BatchSize 로 막았다.
 *   - Problem.problemTags 필드 레벨 @BatchSize(size = 100)
 *   - AlgorithmTag 클래스 레벨 @BatchSize(size = 100)
 * 둘 중 하나라도 사라지면 쿼리 수가 급증한다. 상한을 실측값 그대로(여유 없이) 잡아
 * 그 회귀가 반드시 빨간불로 드러나게 한다.
 *
 * ── 측정 방법 ──
 * Hibernate Statistics 의 getPrepareStatementCount() 를 쓴다.
 * getQueryExecutionCount() 는 HQL/Criteria 만 세고 지연 로딩으로 나가는 select 를 세지 않아
 * 정확히 우리가 잡으려는 것을 놓친다.
 * generate_statistics 는 @TestPropertySource 로 이 테스트에서만 켠다(프로덕션 설정 무수정).
 *
 * ── 데이터 ──
 * Flyway V2 시드(문제 689건 / 문제-태그 858건 / 태그 25건)를 그대로 쓴다. 테스트 데이터를 만들지 않는다.
 * 시드의 created_at 이 전부 동일해 "어느 50건"이 오는지는 비결정적이지만,
 * @BatchSize 가 100 이라 어떤 50건이 와도 쿼리 수는 변하지 않는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ProblemListQueryCountTest {

    // 실측값. 내역은 아래 테스트 주석 참고.
    private static final long MAX_SELECT_QUERIES = 4;

    private static final int PAGE_SIZE = 50;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("문제 목록 한 페이지(50건)를 태그까지 매핑하는 동안 리포지토리 조회 + DTO 매핑 구간의 select가 4개를 넘지 않는다 (서비스의 Redis 캐시 구간은 이 테스트의 범위가 아님)")
    void 목록_한_페이지_조회의_쿼리_수가_상한_이하다() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        // ── 측정 구간 시작 ──
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Problem> page = problemRepository.search(null, null, null, null, null, pageable);

        // 엔드포인트와 동일한 매핑을 태워 problemTags → tag 지연 로딩을 실제로 유발한다.
        // 이 줄이 없으면 태그를 한 번도 건드리지 않아 N+1 이 발생할 기회조차 없다.
        List<ProblemResponse> responses = page.getContent().stream()
                .map(ProblemResponse::from)
                .toList();
        // ── 측정 구간 끝 ──

        long selectCount = statistics.getPrepareStatementCount();

        // 측정값을 눈으로 확인할 수 있게 남긴다. 상한을 실측값 그대로 잡았기 때문에
        // 값이 바뀌었을 때 "몇으로 바뀌었는지"가 바로 보여야 한다.
        System.out.println("[query-count] problem list page(" + PAGE_SIZE + ") select = " + selectCount);

        // 가드 1 — 실제로 50건을 읽었는가.
        // 데이터가 비면 쿼리 수가 저절로 줄어 테스트가 거짓으로 통과한다.
        assertThat(page.getContent())
                .as("Flyway 시드가 적용된 DB 여야 한다 (문제 689건)")
                .hasSize(PAGE_SIZE);

        // 가드 2 — 태그가 실제로 로딩됐는가.
        // 태그를 하나도 안 건드리면 N+1 이 일어날 수 없어 이 테스트가 아무것도 지키지 못한다.
        assertThat(responses)
                .as("태그 지연 로딩이 실제로 일어나야 이 테스트가 의미를 가진다")
                .anyMatch(response -> !response.tags().isEmpty());

        // 본 단정 — 쿼리 수 상한.
        // 실측 내역(총 4):
        //   1. content select  — QueryDSL selectFrom(problem) ... limit 50
        //   2. count select    — QueryDSL select(problem.count())
        //   3. problemTags 컬렉션 배치 로딩 — 50건 ≤ @BatchSize(100) 이라 1회
        //   4. AlgorithmTag 엔티티 배치 로딩 — 서로 다른 태그 17개 ≤ @BatchSize(100) 이라 1회
        // 여유를 두지 않은 것은 의도적이다. 넉넉한 상한은 회귀를 통과시킨다.
        assertThat(selectCount)
                .as("N+1 회귀 감지. 상한을 넘었다면 @BatchSize 가 빠졌거나 지연 로딩이 새로 생긴 것이다")
                .isLessThanOrEqualTo(MAX_SELECT_QUERIES);
    }
}
