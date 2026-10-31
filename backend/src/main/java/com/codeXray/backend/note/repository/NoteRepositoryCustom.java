package com.codeXray.backend.note.repository;

import com.codeXray.backend.note.entity.Note;
import com.codeXray.backend.note.entity.NoteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// QueryDSL 동적 필터 (type + 검색어) + 페이지.
public interface NoteRepositoryCustom {

    Page<Note> search(Long userId, NoteType type, String search, Pageable pageable);
}
