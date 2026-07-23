package com.codeXray.backend.ai.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

/*
 * 사용자별 "1일 N회" AI 분석 한도. RefreshTokenStore 처럼 StringRedisTemplate 재사용.
 * 키: ai:quota:{userId}:{yyyy-MM-dd}, 값: 오늘 사용 횟수. 첫 사용 시 TTL 1일 부여(자동 정리).
 */
@Component
public class AiRateLimiter {

    private static final String KEY_PREFIX = "ai:quota:";

    private final StringRedisTemplate redisTemplate;
    private final long dailyLimit;

    public AiRateLimiter(StringRedisTemplate redisTemplate,
                         @Value("${app.ai.daily-limit:2}") long dailyLimit) {
        this.redisTemplate = redisTemplate;
        this.dailyLimit = dailyLimit;
    }

    // 호출 1회를 원자적으로 카운트. 한도 초과면 429 예외.
    public void checkAndIncrement(Long userId) {
        String key = KEY_PREFIX + userId + ":" + LocalDate.now();

        Long count = redisTemplate.opsForValue().increment(key); // 원자적 INCR, 없으면 1로 생성
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofDays(1)); // 첫 사용에만 만료 부여
        }
        if (count == null || count > dailyLimit) {
            throw new BusinessException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        }
    }
}
