package com.codeXray.backend.ai.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

/*
 * 사용자별 "1일 N회" AI 분석 한도. RefreshTokenStore 처럼 StringRedisTemplate 재사용.
 * 키: ai:quota:{userId}:{yyyy-MM-dd}, 값: 오늘 사용 횟수. 첫 사용 시 TTL 2일 부여(자동 정리).
 * "오늘"은 한국 시간 기준이다. 서버(컨테이너) 기본 시간대가 UTC 라 LocalDate.now() 를 쓰면 한국 사용자의 하루가 오전 9시에 바뀌었다
 */
@Component
public class AiRateLimiter {

    private static final String KEY_PREFIX = "ai:quota:";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redisTemplate;
    private final long dailyLimit;

    public AiRateLimiter(StringRedisTemplate redisTemplate,
                         @Value("${app.ai.daily-limit:2}") long dailyLimit) {
        this.redisTemplate = redisTemplate;
        this.dailyLimit = dailyLimit;
    }

    // 호출 1회를 원자적으로 카운트. 한도 초과면 429 예외.
    public void checkAndIncrement(Long userId) {
        String key = key(userId);

        Long count = redisTemplate.opsForValue().increment(key); // 원자적 INCR, 없으면 1로 생성
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofDays(2)); // 첫 사용에만 만료 부여. 날짜가 키에 있어 하루만 지나면 안 쓰인다
        }
        if (count == null || count > dailyLimit) {
            throw new BusinessException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        }
    }

    // 요청은 받았지만 큐에 넣지 못한 경우 한 번을 돌려준다(사용자 잘못이 아닌 실패로 한도를 깎지 않게)
    public void refund(Long userId) {
        redisTemplate.opsForValue().decrement(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now(KST);
    }
}
