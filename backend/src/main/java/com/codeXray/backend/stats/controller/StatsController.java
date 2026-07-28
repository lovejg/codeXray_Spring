package com.codeXray.backend.stats.controller;

import com.codeXray.backend.stats.dto.DashboardResponse;
import com.codeXray.backend.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    // GET /api/stats/me — 내 대시보드 통계
    @GetMapping("/me")
    public DashboardResponse myDashboard(@AuthenticationPrincipal Long userId) {
        return statsService.getDashboard(userId);
    }
}
