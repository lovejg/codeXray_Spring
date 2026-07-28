package com.codeXray.backend.recommend.controller;

import com.codeXray.backend.recommend.dto.RecommendationResponse;
import com.codeXray.backend.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    // GET /api/recommendations — 내 약점/난이도 기반 추천 문제
    @GetMapping
    public List<RecommendationResponse> recommendations(@AuthenticationPrincipal Long userId) {
        return recommendService.recommend(userId);
    }
}
