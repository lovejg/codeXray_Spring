package com.codeXray.backend.note.repository;

import com.codeXray.backend.note.entity.Note;
import com.codeXray.backend.note.entity.NoteType;

import java.util.List;

// QueryDSL 동적 필터 (type + 검색어). 페이징 없음(내 노트 전량 반환).
public interface NoteRepositoryCustom {

    List<Note> search(Long userId, NoteType type, String search);
}
