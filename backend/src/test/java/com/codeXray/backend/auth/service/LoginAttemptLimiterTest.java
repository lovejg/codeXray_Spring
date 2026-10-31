package com.codeXray.backend.auth.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 로그인 실패 횟수 제한. RefreshTokenStoreTest 처럼 앱을 띄우지 않고 실제 Redis 에 대고 돌린다
class LoginAttemptLimiterTest {

    private static final String EMAIL = "Limiter-Test@Test.Example";

    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        limiter = new LoginAttemptLimiter(redis, 5, 15);
        limiter.reset(EMAIL);
    }

    @AfterEach
    void tearDown() {
        limiter.reset(EMAIL);
        factory.destroy();
    }

    @Test
    @DisplayName("4번 틀려도 로그인할 수 있고, 5번째 실패부터 429")
    void blocksAfterFiveFailures() {
        for (int i = 0; i < 4; i++) limiter.recordFailure(EMAIL);
        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();

        limiter.recordFailure(EMAIL);

        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
    }

    @Test
    @DisplayName("이메일 대소문자를 바꿔도 같은 카운터를 쓴다(우회 방지)")
    void caseInsensitive() {
        for (int i = 0; i < 5; i++) limiter.recordFailure(EMAIL.toUpperCase());

        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL.toLowerCase()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("카운터는 만료 시간이 있다(영구 잠금이 아니다)")
    void counterExpires() {
        limiter.recordFailure(EMAIL);

        assertThat(redis.getExpire("login:fail:" + EMAIL.toLowerCase())).isPositive();
    }
}
