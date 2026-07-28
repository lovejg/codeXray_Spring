package com.codeXray.backend.ai.controller;

import com.codeXray.backend.ai.dto.AiAnalyzeRequest;
import com.codeXray.backend.ai.dto.AiHintRequest;
import com.codeXray.backend.ai.dto.AiJobResponse;
import com.codeXray.backend.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // 풀이 분석 요청 → 잡 생성(PENDING) 후 즉시 반환. 실제 처리는 Kafka 컨슈머가.
    @PostMapping("/analyze")
    public AiJobResponse analyze(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AiAnalyzeRequest req
    ) {
        Long jobId = aiService.requestAnalyze(userId, req.task(), req.code(), req.language(), req.problemTitle());
        return aiService.getJob(userId, jobId);
    }

    // 문제 힌트 요청 → 잡 생성(PENDING) 후 즉시 반환.
    @PostMapping("/hint")
    public AiJobResponse hint(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AiHintRequest req
    ) {
        Long jobId = aiService.requestHint(userId, req.problemId(), req.partialCode());
        return aiService.getJob(userId, jobId);
    }

    // 잡 상태/결과 폴링
    @GetMapping("/jobs/{id}")
    public AiJobResponse job(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return aiService.getJob(userId, id);
    }
}
