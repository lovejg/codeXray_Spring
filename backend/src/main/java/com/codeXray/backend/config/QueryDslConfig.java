package com.codeXray.backend.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 쿼리를 만들 때 쓰는 JPAQueryFactory를 스프링 빈으로 등록.
 * EntityManager(영속성 컨텍스트 창구)를 주입받아 팩토리를 만든다.
 */
@Configuration
public class QueryDslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
