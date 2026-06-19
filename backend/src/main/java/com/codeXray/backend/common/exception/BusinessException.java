package com.codeXray.backend.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 서비스 계층에서 던지는 예외.
 * GlobalExceptionHandler 가 이걸 잡아서 ErrorResponse(JSON)로 변환한다.
 *
 * 사용 예: throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 커스텀 메시지를 쓰고 싶을 때 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
