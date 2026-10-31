package com.codeXray.backend.common.exception;

import com.codeXray.backend.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * 모든 컨트롤러에서 발생한 예외를 한 곳에서 가로채 통일된 JSON으로 변환.
 * (NestJS의 Exception Filter 대응)
 */
@Slf4j
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

    /** 요청 모양이 잘못됨(숫자 자리에 글자, 필수 파라미터 없음, 깨진 JSON) → 400. 클라이언트 잘못이라 500 으로 두지 않는다 */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return simple(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT);
    }

    /**
     * @PreAuthorize 거부. 필터 단계가 아니라 컨트롤러 호출 중에 나오므로 아래 Exception 처리기가 500 으로 바꾸지 않게 따로 받는다.
     * 로그인 안 함 → 401, 로그인했지만 권한 없음 → 403 (필터 단계의 RestAuthenticationEntryPoint/AccessDeniedHandler 와 같은 규칙)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated();
        return anonymous
                ? simple(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED)
                : simple(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
    }

    /** 예상 못한 나머지 모든 예외 → 500 (민감 정보 노출 방지: 상세는 로그로만) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // 없는 주소(404), 안 받는 메서드(405) 같은 스프링 MVC 예외는 자기 상태 코드를 들고 있다. 그대로 돌려준다
        if (e instanceof org.springframework.web.ErrorResponse mvc) {
            HttpStatusCode status = mvc.getStatusCode();
            ErrorCode code = status.value() == 404 ? ErrorCode.NOT_FOUND : ErrorCode.INVALID_INPUT;
            return simple(status, code);
        }
        log.error("처리하지 못한 예외: {}", e.getMessage(), e);
        return simple(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> simple(HttpStatusCode status, ErrorCode code) {
        ErrorResponse body = new ErrorResponse(status.value(), code.name(), code.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }
}
