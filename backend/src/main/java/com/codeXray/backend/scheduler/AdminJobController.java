package com.codeXray.backend.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// [Admin] 예정된 배치를 기다리지 않고 즉시 트리거.
// 권한은 두 겹: SecurityConfig 의 URL 규칙 + 여기의 @PreAuthorize. URL 규칙의 순서를 잘못 바꿔도 이 컨트롤러는 열리지 않는다
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/jobs")
@RequiredArgsConstructor
public class AdminJobController {

    private final SchedulerService scheduler;

    @PostMapping("/tier-recompute")
    public JobResult tierRecompute() {
        return scheduler.tierRecompute();
    }

    @PostMapping("/cleanup-tokens")
    public JobResult cleanupTokens() {
        return scheduler.cleanupTokens();
    }

    @PostMapping("/cleanup-notifications")
    public JobResult cleanupNotifications() {
        return scheduler.cleanupNotifications();
    }

    @PostMapping("/stale-suggestion-digest")
    public JobResult staleSuggestionDigest() {
        return scheduler.staleSuggestionDigest();
    }
}
