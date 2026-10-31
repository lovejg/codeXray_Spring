package com.codeXray.backend.note.repository;

import com.codeXray.backend.note.entity.Note;
import com.codeXray.backend.note.entity.NoteType;
import com.codeXray.backend.note.entity.QNote;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

public class NoteRepositoryCustomImpl implements NoteRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QNote note = QNote.note;

    public NoteRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Note> search(Long userId, NoteType type, String search, Pageable pageable) {
        BooleanExpression[] where = {
                note.userId.eq(userId),   // 항상: 내 노트만
                typeEq(type),              // 선택: 타입 필터
                keywordContains(search)    // 선택: 제목/본문 검색
        };

        List<Note> content = queryFactory
                .selectFrom(note)
                .where(where)
                .orderBy(note.updatedAt.desc(), note.id.desc()) // id 로 순서를 하나로 고정(페이지 경계에서 겹치거나 빠지지 않게)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 첫 페이지가 다 안 찼으면 count 쿼리를 생략한다(전체 개수가 곧 content 크기)
        return PageableExecutionUtils.getPage(content, pageable,
                () -> {
                    Long total = queryFactory.select(note.count()).from(note).where(where).fetchOne();
                    return total == null ? 0 : total;
                });
    }

    private BooleanExpression typeEq(NoteType type) {
        return (type == null) ? null : note.type.eq(type);
    }

    // 제목 OR 본문 부분일치(대소문자 무시). where()에 null이면 무시됨.
    private BooleanExpression keywordContains(String search) {
        return (search == null || search.isBlank())
                ? null
                : note.title.containsIgnoreCase(search).or(note.body.containsIgnoreCase(search));
    }
}
