package com.codeXray.backend.solution.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSolutionRequest(
        @NotBlank String code,
        String language // 디폴트는 python
) {
}
