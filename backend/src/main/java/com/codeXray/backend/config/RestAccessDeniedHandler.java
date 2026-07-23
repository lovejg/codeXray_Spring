package com.codeXray.backend.config;

import com.codeXray.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한이 부족한 경우(예: 일반 유저가 ADMIN 전용 경로 접근) 호출된다.
 *
 * <p>401(인증 필요)이 아니라 403(FORBIDDEN)을 반환한다.
 * EntryPoint(401)와 짝을 이루며, 응답 본문은 공통 포맷(ErrorResponse) 모양으로 직렬화한다.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String body = """
                {"statusCode":%d,"errorCode":"%s","message":"%s","fieldErrors":null}"""
                .formatted(errorCode.getStatus().value(), errorCode.name(), errorCode.getMessage());

        response.getWriter().write(body);
    }
}
