package com.codeXray.backend.community.dto;

import jakarta.validation.constraints.Size;

// 빈 문자열/null 이면 답변 제거. 그래서 @NotBlank 아님.
public record UpdateAdminReplyRequest(
        @Size(max = 5000) String adminReply
) {
}
