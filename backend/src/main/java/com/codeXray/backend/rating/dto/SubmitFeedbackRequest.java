package com.codeXray.backend.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 체감 난이도 0~5 정수. 범위 검증은 여기서(@Min/@Max) → 위반 시 400 INVALID_INPUT
public record SubmitFeedbackRequest(
        @NotNull @Min(0) @Max(5) Integer level
) {
}
