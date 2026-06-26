package com.codeXray.backend.auth.refresh;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/*
 * Refresh Token을 Redis에 저장/조회/삭제하는 저장소.
 * <p>저장 형태: key = "refresh:{토큰값}", value = "{userId}".
 * 토큰값으로 바로 조회해 userId를 알 수 있고, TTL로 14일 뒤 자동 삭제된다.
 */
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final long expirationDays;

    // @Value로 만료일도 같이 주입
    public RefreshTokenStore(
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.refresh-expiration-days}") long expirationDays
    ) {
        this.redisTemplate = redisTemplate;
        this.expirationDays = expirationDays;
    }

    // 새 refresh 토큰을 발급해 Redis에 저장하고, 토큰 문자열을 반환
    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();

        redisTemplate.opsForValue()
                .set(key(token), String.valueOf(userId), Duration.ofDays(expirationDays));

        return token;
    }

    // 토큰으로 userId를 조회. 없으면(만료/위조/로그아웃) 빈 Optional
    public Optional<Long> findUserId(String token) {
        String userId = redisTemplate.opsForValue().get(key(token));
        return Optional.ofNullable(userId).map(Long::valueOf);
    }

    public void delete(String token) {
        redisTemplate.delete(key(token));
    }

    // 토큰값 → 실제 Redis 키(접두사 부착)
    private String key(String token) {
        return KEY_PREFIX + token;
    }
}
