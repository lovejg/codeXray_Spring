package com.codeXray.backend.auth.refresh;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/*
 * Refresh Token을 Redis에 저장/조회/삭제하는 저장소.
 * 저장 형태: key = "refresh:{토큰값}", value = "{userId}".
 * 토큰값으로 바로 조회해 userId를 알 수 있고, TTL(app.jwt.refresh-expiration-days, 운영 14일)이 지나면 자동 삭제된다.
 * 사용자별 목록 "refresh-user:{userId}"(Set)에도 토큰을 넣어 둔다. 비밀번호를 바꾸면 이 목록으로 모든 기기의 토큰을 지운다.
 * 목록에는 이미 만료된 토큰이 남아 있을 수 있는데, 지울 때 없는 키를 지우는 것뿐이라 상관없다.
 */
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";
    private static final String USER_KEY_PREFIX = "refresh-user:";

    private final StringRedisTemplate redisTemplate;
    private final long expirationDays;

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
        Duration ttl = Duration.ofDays(expirationDays);

        redisTemplate.opsForValue().set(key(token), String.valueOf(userId), ttl);
        // 목록은 가장 최근 토큰만큼 살아 있으면 된다. 발급할 때마다 수명을 다시 늘린다
        redisTemplate.opsForSet().add(userKey(userId), token);
        redisTemplate.expire(userKey(userId), ttl);

        return token;
    }

    // 토큰으로 userId를 조회. 없으면(만료/위조/로그아웃) 빈 Optional
    public Optional<Long> findUserId(String token) {
        String userId = redisTemplate.opsForValue().get(key(token));
        return Optional.ofNullable(userId).map(Long::valueOf);
    }

    public void delete(String token) {
        findUserId(token).ifPresent(userId -> redisTemplate.opsForSet().remove(userKey(userId), token));
        redisTemplate.delete(key(token));
    }

    // 그 사용자의 모든 기기 토큰을 지운다. 비밀번호 변경, 탈퇴 때
    public void deleteAllOf(Long userId) {
        Set<String> tokens = redisTemplate.opsForSet().members(userKey(userId));
        if (tokens != null && !tokens.isEmpty()) {
            redisTemplate.delete(tokens.stream().map(this::key).toList());
        }
        redisTemplate.delete(userKey(userId));
    }

    // 토큰값 → 실제 Redis 키(접두사 부착)
    private String key(String token) {
        return KEY_PREFIX + token;
    }

    private String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }
}
