package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.entity.QCommunityPost;
import com.codeXray.backend.community.entity.SuggestionStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;

public class CommunityPostRepositoryCustomImpl implements CommunityPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QCommunityPost post = QCommunityPost.communityPost;

    public CommunityPostRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<CommunityPost> findVisiblePosts(Long userId, boolean isAdmin,
                                                List<PostType> types, Long problemId,
                                                SuggestionStatus status, Long authorId) {
        return queryFactory
                .selectFrom(post)
                // 목록에서 작성자 닉네임/문제 제목을 함께 쓰므로 미리 조인 로딩 (N+1 방지). nullable 이라 leftJoin.
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.problem).fetchJoin()
                .where(
                        visibility(userId, isAdmin),
                        typeIn(types),
                        problemEq(problemId),
                        statusEq(status),
                        authorEq(authorId)
                )
                .orderBy(post.createdAt.desc())
                .fetch();
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
