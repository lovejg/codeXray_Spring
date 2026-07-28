package com.codeXray.backend.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 문제 기반 힌트 요청. partialCode(막힌 코드)는 선택.
public record AiHintRequest(
        @NotNull Long problemId,
        @Size(max = 20000) String partialCode
) {
}
