package com.codeXray.backend.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// [Admin] 예정된 배치를 기다리지 않고 즉시 트리거. SecurityConfig 에서 ROLE_ADMIN 강제.
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
