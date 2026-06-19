package com.codeXray.backend.common.dto;

import java.util.List;

/**
 * 모든 에러 응답의 공통 포맷.
 * { statusCode, errorCode, message, fieldErrors? }
 *
 * record = 불변 DTO를 간결하게 만드는 Java 16+ 문법 (Node의 plain object 같은 역할).
 */
public record ErrorResponse(
        int statusCode,
        String errorCode,
        String message,
        List<FieldError> fieldErrors   // 검증 실패 시에만 채워짐, 아니면 null
) {
    public record FieldError(String field, String message) {}
}
