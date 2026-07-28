package com.codeXray.backend.recommend.dto;

import com.codeXray.backend.problem.dto.ProblemResponse;

// 추천 문제 1건: 문제 + 추천 이유(약점 보강/적정 난이도) + 매칭된 약점 태그(있으면)
public record RecommendationResponse(
        ProblemResponse problem,
        String reason,
        String tag
) {
}
