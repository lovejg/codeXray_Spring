package com.codeXray.backend.common.exception;

import com.codeXray.backend.problem.controller.ProblemController;
import com.codeXray.backend.problem.service.ProblemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 잘못된 요청이 500 이 아니라 알맞은 상태 코드로 돌아오는지.
// 예전에는 숫자 자리에 글자를 넣거나 없는 주소를 부르면 전부 500 이었다
class GlobalExceptionHandlerTest {

    private final ProblemService problemService = mock(ProblemService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProblemController(problemService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();

    @Test
    @DisplayName("id 자리에 숫자가 아니면 400")
    void typeMismatchIs400() throws Exception {
        mvc.perform(get("/api/problems/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("필수 파라미터가 없으면 400")
    void missingParamIs400() throws Exception {
        mvc.perform(get("/api/problems/lookup"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 주소는 404")
    void unknownPathIs404() throws Exception {
        mvc.perform(get("/api/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("@PreAuthorize 거부(AccessDeniedException)는 500 이 아니라 로그인 안 했으면 401")
    void accessDeniedIsNot500() throws Exception {
        when(problemService.getById(anyLong()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("denied"));

        mvc.perform(get("/api/problems/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("예상 못 한 예외는 500 이고 내부 메시지를 내보내지 않는다")
    void unexpectedIs500() throws Exception {
        when(problemService.getById(anyLong())).thenThrow(new IllegalStateException("db password is hunter2"));

        mvc.perform(get("/api/problems/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
