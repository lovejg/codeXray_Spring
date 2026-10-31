package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.entity.QCommunityPost;
import com.codeXray.backend.community.entity.QPostVote;
import com.codeXray.backend.community.entity.SuggestionStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

public class CommunityPostRepositoryCustomImpl implements CommunityPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QCommunityPost post = QCommunityPost.communityPost;

    public CommunityPostRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<CommunityPost> findVisiblePosts(Long userId, boolean isAdmin,
                                                List<PostType> types, Long problemId,
                                                SuggestionStatus status, Long authorId,
                                                boolean byVotes, Pageable pageable) {
        Predicate[] where = {
                visibility(userId, isAdmin),
                typeIn(types),
                problemEq(problemId),
                statusEq(status),
                authorEq(authorId)
        };

        List<CommunityPost> content = queryFactory
                .selectFrom(post)
                // 목록에서 작성자 닉네임/문제 제목을 함께 쓰므로 미리 조인 로딩 (N+1 방지). nullable 이라 leftJoin.
                // to-one 만 fetch join 하므로 limit/offset 이 SQL 에서 그대로 걸린다
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.problem).fetchJoin()
                .where(where)
                .orderBy(orders(byVotes))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = queryFactory.select(post.count()).from(post).where(where).fetchOne();
            return total == null ? 0 : total;
        });
    }

    /*
     * 추천순은 글마다 추천 합계(+1/-1)를 상관 서브쿼리로 계산해 DB 에서 정렬한다.
     * 예전에는 보이는 글 전부를 메모리에 올린 뒤 Java 로 정렬해서, 페이지를 나눌 수 없었다(어느 글이 1페이지인지 전부 봐야 앎).
     * 점수가 같으면 최신 글, 그다음 id 로 순서를 하나로 고정한다
     */
    private OrderSpecifier<?>[] orders(boolean byVotes) {
        if (!byVotes) {
            return new OrderSpecifier<?>[]{post.createdAt.desc(), post.id.desc()};
        }
        QPostVote vote = QPostVote.postVote;
        NumberExpression<Integer> score = Expressions.numberTemplate(Integer.class, "coalesce(({0}), 0)",
                JPAExpressions.select(Expressions.numberTemplate(Integer.class, "sum({0})", vote.value))
                        .from(vote).where(vote.postId.eq(post.id)));
        return new OrderSpecifier<?>[]{score.desc(), post.createdAt.desc(), post.id.desc()};
    }

    // 비공개/숨김 접근 규칙:
    //  - 관리자: 전부 조회
    //  - 로그인: (공개 OR 내 글) AND (숨김아님 OR 내 글)
    //  - 비로그인: 공개 AND 숨김아님
    private BooleanBuilder visibility(Long userId, boolean isAdmin) {
        BooleanBuilder b = new BooleanBuilder();
        if (isAdmin) return b; // 제약 없음
        if (userId != null) {
            b.and(post.isPrivate.isFalse().or(post.user.id.eq(userId)));
            b.and(post.hidden.isFalse().or(post.user.id.eq(userId)));
        } else {
            b.and(post.isPrivate.isFalse());
            b.and(post.hidden.isFalse());
        }
        return b;
    }

    private BooleanExpression typeIn(List<PostType> types) {
        return (types == null || types.isEmpty()) ? null : post.type.in(types);
    }

    private BooleanExpression problemEq(Long problemId) {
        return (problemId == null) ? null : post.problem.id.eq(problemId);
    }

    private BooleanExpression statusEq(SuggestionStatus status) {
        return (status == null) ? null : post.status.eq(status);
    }

    private BooleanExpression authorEq(Long authorId) {
        return (authorId == null) ? null : post.user.id.eq(authorId);
    }
}
