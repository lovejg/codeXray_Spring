package com.codeXray.backend.solution.repository;

import com.codeXray.backend.config.QueryDslConfig;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.repository.ProblemRepository;
import com.codeXray.backend.solution.dto.SolutionResponse;
import com.codeXray.backend.solution.entity.Memo;
import com.codeXray.backend.solution.entity.Solution;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 내 풀이 목록 한 페이지의 select 수를 고정한다.
 *
 * 고치기 전(파생 쿼리 findByUserIdOrderByUpdatedAtDesc + 지연 로딩)에는 풀이 20건에 select 72개였다
 * (2026-11-01, 이 테스트와 같은 데이터·같은 계수기로 측정. 2026-09 재분석은 HTTP 요청 1회를 PostgreSQL 로그로 세어 73).
 *   문제 20 + 메모 20 + 문제 태그 20 + 태그 이름 개별·배치 + 목록 1. 행당 약 3.5개.
 * 메모가 하나도 없어도 메모 조회 20번이 나갔다(역방향 @OneToOne 은 LAZY 가 동작하지 않는다).
 *
 * 지금은 풀이·문제·메모를 join fetch 한 쿼리로 읽고, 태그는 @BatchSize 로 페이지 전체를 묶는다.
 * 상한은 실측값 그대로 4. 풀이 수가 늘어도 변하지 않아야 한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class SolutionListQueryCountTest {

    private static final long MAX_SELECT_QUERIES = 4;
    private static final int ROWS = 20;

    @Autowired UserRepository userRepository;
    @Autowired ProblemRepository problemRepository;
    @Autowired SolutionRepository solutionRepository;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;

    @Test
    @DisplayName("내 풀이 20건 한 페이지를 문제·태그·메모까지 매핑해도 select 는 4개를 넘지 않는다(고치기 전 72)")
    void solutionPageQueryCount() {
        User user = userRepository.save(User.builder()
                .email("solution-n1@test.example").password("x").nickname("solution-n1")
                .role(UserRole.USER).provider(AuthProvider.LOCAL).build());

        // 시드 문제 중 태그가 있는 앞쪽 20개에 풀이를 달고, 절반에만 메모를 붙인다
        List<Problem> problems = problemRepository.findAll(PageRequest.of(0, ROWS)).getContent();
        for (int i = 0; i < problems.size(); i++) {
            Solution s = solutionRepository.save(Solution.builder()
                    .userId(user.getId()).problem(problems.get(i)).code("print(" + i + ")").language("python").build());
            if (i % 2 == 0) {
                em.persist(Memo.builder().solution(s).wrongReason("r" + i).build());
            }
        }
        em.flush();
        em.clear(); // 영속성 컨텍스트를 비워야 실제 조회 쿼리가 나간다

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        // ── 측정 구간: 컨트롤러와 같은 조회 + 응답 매핑 ──
        Page<Solution> page = solutionRepository.findPageByUserId(user.getId(), PageRequest.of(0, ROWS));
        List<SolutionResponse> responses = page.getContent().stream().map(SolutionResponse::from).toList();
        // ──

        long selects = stats.getPrepareStatementCount();
        System.out.println("[query-count] solution list page(" + ROWS + ") select = " + selects);

        assertThat(responses).hasSize(ROWS);
        assertThat(responses).filteredOn(r -> r.memo() != null).hasSize(ROWS / 2);
        assertThat(responses).anyMatch(r -> !r.problem().tags().isEmpty());
        assertThat(selects)
                .as("N+1 회귀 감지. 문제/메모 fetch join 이나 태그 @BatchSize 가 빠졌는지 확인")
                .isLessThanOrEqualTo(MAX_SELECT_QUERIES);
    }
}
