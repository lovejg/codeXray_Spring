package com.codeXray.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotBlank @Size(min = 2, max = 500) String reason
) {
}
