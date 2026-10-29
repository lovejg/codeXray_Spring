package com.codeXray.backend.scheduler;

import com.codeXray.backend.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/*
 * 앱이 뜰 때 티어가 비어 있는 문제가 있으면 전체 티어 재계산을 한 번 돌린다.
 *
 * 문제 시드(Flyway V2)는 레벨·정답률만 넣고 adjusted_level/tier 는 비워 둔다. 재계산은 매주 일 03:00 cron,
 * 피드백 제출(그 문제 하나), 관리자 수동 트리거에서만 일어나서, 새 DB(첫 배포 등)에서는 다음 일요일까지
 * 모든 문제의 티어가 비어 목록의 티어 표시와 티어 구간 추천이 비었다(2026-10-29 운영 첫 배포에서 확인).
 * 이미 다 계산돼 있으면 아무것도 하지 않는다. 전체 재계산은 문제 689개 기준 1초 안쪽이다.
 */
@Component
@RequiredArgsConstructor
public class TierBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TierBootstrap.class);

    private final ProblemRepository problemRepository;
    private final SchedulerService scheduler;

    @Override
    public void run(ApplicationArguments args) {
        if (problemRepository.existsByTierIsNull()) {
            log.info("티어가 비어 있는 문제가 있어 전체 재계산을 한 번 돌린다");
            // 프록시 빈을 거쳐 부르므로 @Transactional 과 캐시 무효화(@CacheEvict)가 그대로 적용된다
            scheduler.tierRecompute();
        }
    }
}
