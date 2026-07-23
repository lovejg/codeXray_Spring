package com.codeXray.backend.ai.controller;

import com.codeXray.backend.ai.dto.AiAnalyzeRequest;
import com.codeXray.backend.ai.dto.AiAnalyzeResponse;
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

    // 풀이 분석(Claude). 인증 필요 + 사용자당 1일 한도.
    @PostMapping("/analyze")
    public AiAnalyzeResponse analyze(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AiAnalyzeRequest req
    ) {
        String result = aiService.analyze(userId, req.task(), req.code(), req.language(), req.problemTitle());
        return new AiAnalyzeResponse(result);
    }
}
