package com.codeXray.backend.rating.controller;

import com.codeXray.backend.rating.dto.FeedbackResponse;
import com.codeXray.backend.rating.dto.RecomputeResponse;
import com.codeXray.backend.rating.dto.SubmitFeedbackRequest;
import com.codeXray.backend.rating.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // 난이도 피드백 제출(0~5) → 해당 문제 tier 자동 재계산
    @PostMapping("/feedback/{problemId}")
    public FeedbackResponse submitFeedback(
            @PathVariable Long problemId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubmitFeedbackRequest request
    ) {
        return ratingService.submitFeedback(userId, problemId, request.level());
    }

    // 특정 문제에 대한 내 피드백 (없으면 null)
    @GetMapping("/feedback/{problemId}")
    public FeedbackResponse getMyFeedback(
            @PathVariable Long problemId,
            @AuthenticationPrincipal Long userId
    ) {
        return ratingService.getMyFeedback(userId, problemId);
    }

    // [Admin] 전체 문제 tier 재계산 (주간 배치 수동 트리거). ROLE_ADMIN 필요
    @PostMapping("/recompute-all")
    public RecomputeResponse recomputeAll() {
        return new RecomputeResponse(ratingService.recomputeAll());
    }
}
