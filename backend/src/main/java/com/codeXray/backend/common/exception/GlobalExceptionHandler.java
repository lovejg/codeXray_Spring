package com.codeXray.backend.common.exception;

import com.codeXray.backend.common.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 모든 컨트롤러에서 발생한 예외를 한 곳에서 가로채 통일된 JSON으로 변환.
 * (NestJS의 Exception Filter 대응)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 우리가 의도적으로 던진 비즈니스 예외 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        ErrorResponse body = new ErrorResponse(
                code.getStatus().value(),
                code.name(),
                e.getMessage(),
                null
        );
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    /** @Valid 검증 실패 (DTO의 @NotBlank 등 위반) → 400 + 필드별 에러 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse body = new ErrorResponse(
                400,
                ErrorCode.INVALID_INPUT.name(),
                ErrorCode.INVALID_INPUT.getMessage(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    /** 예상 못한 나머지 모든 예외 → 500 (민감 정보 노출 방지: 상세는 로그로만) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // TODO(Stage 14): 여기에 로깅 추가 (e.getMessage(), stacktrace)
        ErrorResponse body = new ErrorResponse(
                500,
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.getMessage(),
                null
        );
        return ResponseEntity.internalServerError().body(body);
    }
}
