package com.codeXray.backend.problem.repository;

import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.entity.ProblemSource;
import com.codeXray.backend.problem.entity.QProblem;
import com.codeXray.backend.problem.entity.QProblemTag;
import com.codeXray.backend.problem.entity.Tier;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

// 동적 쿼리를 위한 QueryDSL
// 필터(검색어, 출처, 티어, 알고리즘)를 조건에 맞게 골라 붙여서, 문제 목록 조회
// 헬퍼가 null 을 돌려주면 where 에서 그 조건은 빠진다. 값이 있는 조건만 and 로 이어진다
public class ProblemRepositoryCustomImpl implements ProblemRepositoryCustom {

    // 쿼리를 만드는 시작점. selectFrom(problem) 은 SQL 의 select ... from problems 에 해당
    private final JPAQueryFactory queryFactory;

    // Q타입: 쿼리에서 problem.title처럼 필드를 타입 안전하게 참조하는 진입점(테이블 컬럼을 자바 객체로 참조)
    private static final QProblem problem = QProblem.problem;

    public ProblemRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Problem> search(String keyword, ProblemSource source, Integer tierMin,
                                Integer tierMax, Long tagId, Pageable pageable) {

        // 필터링된 문제 리스트(페이지 단위)
        List<Problem> content = queryFactory
                .selectFrom(problem)
                .where(
                        titleContains(keyword),
                        sourceEq(source),
                        tierBetween(tierMin, tierMax),
                        hasTag(tagId)
                )
                .orderBy(toOrders(pageable.getSort()))
                .offset(pageable.getOffset())        // 몇 개 건너뛰고
                .limit(pageable.getPageSize())       // 몇 개 가져올지
                .fetch();

        // 필터링된 문제 전체 개수(페이지 단위 X)
        Long total = queryFactory
                .select(problem.count())
                .from(problem)
                .where(
                        titleContains(keyword),
                        sourceEq(source),
                        tierBetween(tierMin, tierMax),
                        hasTag(tagId)
                )
                .fetchOne();

        // 위 검색 결과를 Page 객체로 만들기(content, totalElements, totalpages)
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // ---------- 헬퍼(BooleanExpression) ----------

    // 제목 검색(대소문자 무시 부분일치)
    private BooleanExpression titleContains(String keyword) {
        return (keyword == null || keyword.isBlank())
                ? null
                : problem.title.containsIgnoreCase(keyword);
    }

    // 티어 범위(ordinal 구간). 범위 밖 숫자는 양 끝으로 맞추고, 최소가 최대보다 크면 결과 없음
    private BooleanExpression tierBetween(Integer tierMin, Integer tierMax) {
        if (tierMin == null && tierMax == null) return null;
        int last = Tier.values().length - 1;
        int from = (tierMin != null) ? Math.clamp(tierMin, 0, last) : 0;
        int to   = (tierMax != null) ? Math.clamp(tierMax, 0, last) : last;
        if (from > to) return problem.id.isNull(); // 항상 거짓인 조건
        List<Tier> range = Arrays.asList(Tier.values()).subList(from, to + 1);
        return problem.tier.in(range);
    }

    // 문제 출처 여부 일치 확인
    private BooleanExpression sourceEq(ProblemSource source) {
        return (source == null) ? null : problem.source.eq(source);
    }

    // 특정 태그를 가진 문제(EXISTS 서브쿼리)
    private BooleanExpression hasTag(Long tagId) {
        if (tagId == null) return null;
        QProblemTag pt = QProblemTag.problemTag;
        return JPAExpressions.selectOne()      // "존재하는 행이 있냐"만 볼 거라 selectOne()
                .from(pt)
                .where(pt.problem.eq(problem)   // 이 problem을 가리키고
                        .and(pt.tag.id.eq(tagId))) // 그 태그가 tagId인
                .exists();                      // ProblemTag가 하나라도 존재?
    }

    // 정렬해도 되는 필드. 이 밖의 이름(없는 필드, 다른 엔티티 경로)은 무시한다.
    // 예전에는 ?sort= 값을 그대로 경로로 만들어 없는 필드면 500 이 났다
    private static final Set<String> SORTABLE = Set.of("createdAt", "level", "acceptanceRate", "adjustedLevel", "title", "id");

    // Pageable(Spring)의 정렬 정보를 QueryDSL의 정렬 정보(OrderSpecifier)로 변환
    @SuppressWarnings({"rawtypes", "unchecked"})
    private OrderSpecifier<?>[] toOrders(Sort sort) {
        PathBuilder<Problem> path = new PathBuilder<>(Problem.class, "problem");
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order o : sort) {
            if (!SORTABLE.contains(o.getProperty())) continue;
            Order dir = o.isAscending() ? Order.ASC : Order.DESC;
            orders.add(new OrderSpecifier(dir, path.getComparable(o.getProperty(), Comparable.class),
                    OrderSpecifier.NullHandling.NullsLast));
        }
        // 마지막에 id 를 붙여 순서를 하나로 정한다. 시드 689문제는 created_at 이 전부 같아서, 이게 없으면
        // 같은 페이지 요청이 매번 다른 50문제를 돌려줄 수 있었다(페이지를 넘기면 겹치거나 빠지는 문제, 캐시 키는 같은데 내용이 다름)
        orders.add(problem.id.desc());
        return orders.toArray(new OrderSpecifier[0]);
    }
}
