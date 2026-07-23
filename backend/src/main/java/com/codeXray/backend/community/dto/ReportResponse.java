package com.codeXray.backend.community.dto;

import com.codeXray.backend.community.entity.PostReport;
import com.codeXray.backend.community.entity.ReportStatus;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        String reason,
        ReportStatus status,
        String adminNote,
        AuthorResponse reporter,
        ReportedPostResponse post,
        LocalDateTime createdAt
) {
    public static ReportResponse from(PostReport r) {
        return new ReportResponse(
                r.getId(),
                r.getReason(),
                r.getStatus(),
                r.getAdminNote(),
                AuthorResponse.from(r.getReporter()),
                ReportedPostResponse.from(r.getPost()),
                r.getCreatedAt()
        );
    }
}
