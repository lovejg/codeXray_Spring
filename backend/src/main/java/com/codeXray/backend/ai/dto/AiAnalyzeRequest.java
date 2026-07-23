package com.codeXray.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Claude 비용 통제를 위해 코드 길이 상한(20000자)
public record AiAnalyzeRequest(
        @NotNull AiTaskType task,
        @NotBlank @Size(max = 20000) String code,
        @Size(max = 30) String language,
        @Size(max = 200) String problemTitle
) {
}
