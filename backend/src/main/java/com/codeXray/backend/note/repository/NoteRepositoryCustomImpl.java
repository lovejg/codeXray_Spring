package com.codeXray.backend.note.repository;

import com.codeXray.backend.note.entity.Note;
import com.codeXray.backend.note.entity.NoteType;
import com.codeXray.backend.note.entity.QNote;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;

public class NoteRepositoryCustomImpl implements NoteRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QNote note = QNote.note;

    public NoteRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Note> search(Long userId, NoteType type, String search) {
        return queryFactory
                .selectFrom(note)
                .where(
                        note.userId.eq(userId),   // 항상: 내 노트만
                        typeEq(type),              // 선택: 타입 필터
                        keywordContains(search)    // 선택: 제목/본문 검색
                )
                .orderBy(note.updatedAt.desc())
                .fetch();
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
