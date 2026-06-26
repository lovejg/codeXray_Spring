package com.codeXray.backend.config;

import com.codeXray.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증이 안 된 채로 보호된 경로에 접근했을 때 호출된다.
 *
 * <p>기본 동작은 403이지만, 우리는 401(UNAUTHORIZED)을 반환한다.
 * 프론트 axios interceptor가 "401이면 토큰 만료 → /auth/refresh 시도"로 단순하게 동작하도록 하기 위함.
 * 응답 본문은 프로젝트 공통 포맷(ErrorResponse)과 동일한 모양으로 직접 직렬화한다.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // ErrorResponse { statusCode, errorCode, message, fieldErrors } 와 동일한 모양
        String body = """
                {"statusCode":%d,"errorCode":"%s","message":"%s","fieldErrors":null}"""
                .formatted(errorCode.getStatus().value(), errorCode.name(), errorCode.getMessage());

        response.getWriter().write(body);
    }
}
