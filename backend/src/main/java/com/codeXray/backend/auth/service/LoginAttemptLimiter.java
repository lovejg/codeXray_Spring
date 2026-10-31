package com.codeXray.backend.auth.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/*
 * 비밀번호 대입(무차별 대입) 방지. 이메일별로 실패 횟수를 Redis 에 센다.
 * 키: login:fail:{소문자 이메일}, 값: 실패 횟수, 첫 실패부터 15분 뒤 자동 삭제.
 * 15분 안에 5번 틀리면 그 이메일로는 남은 시간 동안 로그인을 받지 않는다(맞는 비밀번호여도).
 * 성공하면 카운터를 지운다.
 *
 * 대가: 남의 이메일로 일부러 5번 틀려 그 사람을 15분 막을 수 있다. 계정을 영구 잠그지 않고
 * 시간으로 풀리게 해서 그 피해를 15분으로 한정했다
 */
@Component
public class LoginAttemptLimiter {

    private static final String KEY_PREFIX = "login:fail:";

    private final StringRedisTemplate redisTemplate;
    private final int maxFailures;
    private final Duration window;

    public LoginAttemptLimiter(StringRedisTemplate redisTemplate,
                               @Value("${app.auth.login-max-failures:5}") int maxFailures,
                               @Value("${app.auth.login-lock-minutes:15}") long lockMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxFailures = maxFailures;
        this.window = Duration.ofMinutes(lockMinutes);
    }

    // 비밀번호를 확인하기 전에 부른다. 이미 한도를 넘었으면 429
    public void checkAllowed(String email) {
        String count = redisTemplate.opsForValue().get(key(email));
        if (count != null && Long.parseLong(count) >= maxFailures) {
            throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }
    }

    public void recordFailure(String email) {
        String key = key(email);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
    }

    public void reset(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return KEY_PREFIX + email.trim().toLowerCase(Locale.ROOT);
    }
}
