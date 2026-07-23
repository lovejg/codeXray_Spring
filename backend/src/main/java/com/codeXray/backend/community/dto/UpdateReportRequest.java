package com.codeXray.backend.community.dto;

import com.codeXray.backend.community.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReportRequest(
        @NotNull ReportStatus status,
        @Size(max = 500) String adminNote
) {
}
