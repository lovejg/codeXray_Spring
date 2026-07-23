package com.codeXray.backend.scheduler;

import com.codeXray.backend.auth.repository.EmailVerificationTokenRepository;
import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.repository.CommunityPostRepository;
import com.codeXray.backend.notification.entity.NotificationType;
import com.codeXray.backend.notification.repository.NotificationRepository;
import com.codeXray.backend.notification.service.NotificationService;
import com.codeXray.backend.rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
 * 주기 배치. cron 트리거 메서드는 self 프록시로 @Transactional 작업 메서드를 호출(자기호출 시 프록시 우회 방지).
 * 작업 메서드는 AdminJobController 의 수동 트리거에서도 재사용.
 */
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private static final String TZ = "Asia/Seoul";
    private static final List<PostType> SUGGESTION_TYPES =
            List.of(PostType.FEEDBACK, PostType.BUG_REPORT, PostType.FEATURE_REQUEST);

    private final RatingService ratingService;
    private final NotificationService notificationService;
    private final EmailVerificationTokenRepository tokenRepository;
    private final NotificationRepository notificationRepository;
    private final CommunityPostRepository postRepository;
    private final ObjectProvider<SchedulerService> self; // 프록시 경유용 self 주입

    // ── cron 트리거(프록시 경유하여 @Transactional 작업 실행) ──
    @Scheduled(cron = "0 0 3 * * SUN", zone = TZ)   // 매주 일 03:00 KST
    void cronTierRecompute() { self.getObject().tierRecompute(); }

    @Scheduled(cron = "0 0 4 * * *", zone = TZ)     // 매일 04:00 KST
    void cronCleanupTokens() { self.getObject().cleanupTokens(); }

    @Scheduled(cron = "0 30 4 * * *", zone = TZ)    // 매일 04:30 KST
    void cronCleanupNotifications() { self.getObject().cleanupNotifications(); }

    @Scheduled(cron = "0 0 9 * * MON", zone = TZ)   // 매주 월 09:00 KST
    void cronStaleDigest() { self.getObject().staleSuggestionDigest(); }

    // ── 실제 작업 (수동 트리거에서도 호출) ──
    @Transactional
    public JobResult tierRecompute() {
        return run("tier-recompute", () -> Map.of("count", ratingService.recomputeAll()));
    }

    @Transactional
    public JobResult cleanupTokens() {
        return run("cleanup-tokens", () -> {
            int deleted = tokenRepository.deleteStale(LocalDateTime.now().minusDays(30));
            return Map.of("deleted", deleted);
        });
    }

    @Transactional
    public JobResult cleanupNotifications() {
        return run("cleanup-notifications", () -> {
            int deleted = notificationRepository.deleteReadOlderThan(LocalDateTime.now().minusDays(60));
            return Map.of("deleted", deleted);
        });
    }

    @Transactional
    public JobResult staleSuggestionDigest() {
        return run("stale-suggestion-digest", () -> {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            List<CommunityPost> stale = postRepository.findStaleSuggestions(SUGGESTION_TYPES, cutoff);
            if (stale.isEmpty()) return Map.of("count", 0, "notified", 0);

            CommunityPost oldest = stale.get(0);
            long oldestDays = Duration.between(oldest.getCreatedAt(), LocalDateTime.now()).toDays();
            notificationService.createForAllAdmins(NotificationType.STALE_SUGGESTION,
                    Map.<String, Object>of(
                            "count", stale.size(),
                            "oldestPostId", oldest.getId(),
                            "oldestTitle", oldest.getTitle(),
                            "oldestDays", oldestDays),
                    null);
            return Map.of("count", stale.size());
        });
    }

    // 공통 로깅 + 에러 격리 (한 작업이 실패해도 예외를 삼켜 다른 스케줄에 영향 X)
    private JobResult run(String name, Supplier<Object> work) {
        long start = System.currentTimeMillis();
        log.info("▶ [{}] 시작", name);
        try {
            Object result = work.get();
            log.info("✓ [{}] 완료 ({}ms) {}", name, System.currentTimeMillis() - start, result);
            return new JobResult(name, true, result, null);
        } catch (Exception e) {
            log.error("✗ [{}] 실패: {}", name, e.getMessage(), e);
            return new JobResult(name, false, null, e.getMessage());
        }
    }
}
