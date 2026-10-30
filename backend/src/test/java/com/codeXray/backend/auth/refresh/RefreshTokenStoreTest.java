package com.codeXray.backend.auth.refresh;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// 사용자별 토큰 목록이 맞게 유지되는지. 앱 전체를 띄우지 않고 실제 Redis(REDIS_HOST, REDIS_PORT)에 대고 돌린다
class RefreshTokenStoreTest {

    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private RefreshTokenStore store;

    // 다른 테스트나 로컬 데이터와 부딪히지 않게 크게 잡은 id
    private static final long USER = 990_001L;
    private static final long OTHER = 990_002L;

    @BeforeEach
    void setUp() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        store = new RefreshTokenStore(redis, 14);
    }

    @AfterEach
    void tearDown() {
        store.deleteAllOf(USER);
        store.deleteAllOf(OTHER);
        factory.destroy();
    }

    @Test
    @DisplayName("모든 기기 로그아웃은 그 사용자의 토큰만 지운다")
    void deleteAllOfRemovesOnlyThatUsersTokens() {
        String phone = store.issue(USER);
        String laptop = store.issue(USER);
        String someoneElse = store.issue(OTHER);

        store.deleteAllOf(USER);

        assertThat(store.findUserId(phone)).isEmpty();
        assertThat(store.findUserId(laptop)).isEmpty();
        assertThat(store.findUserId(someoneElse)).contains(OTHER);
    }

    @Test
    @DisplayName("토큰 하나를 지우면(로그아웃, 갱신) 사용자 목록에서도 빠진다")
    void deleteRemovesFromUserList() {
        String old = store.issue(USER);
        String current = store.issue(USER);

        store.delete(old);

        assertThat(redis.opsForSet().members("refresh-user:" + USER)).containsExactly(current);
    }

    @Test
    @DisplayName("사용자 목록도 토큰과 같은 수명을 가진다(영원히 남지 않는다)")
    void userListExpires() {
        store.issue(USER);

        assertThat(redis.getExpire("refresh-user:" + USER)).isPositive();
    }
}
