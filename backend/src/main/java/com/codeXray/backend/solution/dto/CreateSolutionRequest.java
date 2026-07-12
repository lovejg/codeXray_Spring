package com.codeXray.backend.solution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSolutionRequest(
        @NotNull Long problemId,
        @NotBlank String code,
        String language // 디폴트는 python
) {
}
