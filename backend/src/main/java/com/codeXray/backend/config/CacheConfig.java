package com.codeXray.backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Map;

/*
 * 조회 캐시 설정. Redis를 캐시 저장소로 쓴다(refresh 토큰과 같은 Redis 인스턴스, DB만 분리).
 *
 * 두 가지 포인트:
 *  1) 직렬화 — 기본 JDK 직렬화 대신 JSON(GenericJackson2Json). 사람이 읽을 수 있고 언어 중립적이며,
 *     redis-cli 로 값 확인이 쉽다. @class 타입정보를 함께 저장해 record/제네릭도 원형 복원된다.
 *  2) 캐시별 TTL — 데이터 성격에 맞춰 만료를 다르게. 목록/상세는 30분(이벤트 evict가 주력, TTL은 안전망),
 *     태그는 1시간(런타임에 거의 안 변하는 참조 데이터, TTL-only 전략).
 *
 * RedisCacheManager 빈을 직접 정의한다. Spring Boot의 캐시 자동설정에 의존하지 않아
 * 버전에 따라 이동하는 autoconfig 클래스(RedisCacheManagerBuilderCustomizer 등)와 무관하게 동작한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // 캐시 이름 상수 — @Cacheable/@CacheEvict 와 아래 TTL 설정이 같은 이름을 공유하도록 한곳에서 관리
    public static final String PROBLEM_LIST = "problemList";
    public static final String PROBLEM_DETAIL = "problemDetail";
    public static final String TAG_LIST = "tagList";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 모든 캐시의 공통 베이스: JSON 값 직렬화 + null 캐싱 금지(캐시 관통/오염 방지)
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 캐시 이름별 TTL
        Map<String, RedisCacheConfiguration> perCache = Map.of(
                PROBLEM_LIST, base.entryTtl(Duration.ofMinutes(30)),
                PROBLEM_DETAIL, base.entryTtl(Duration.ofMinutes(30)),
                TAG_LIST, base.entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(10))) // 정의 안 된 캐시의 기본 TTL
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
