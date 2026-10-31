package com.codeXray.backend.note.service;

import com.codeXray.backend.common.dto.PageResponse;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.note.dto.NoteResponse;
import com.codeXray.backend.note.entity.Note;
import com.codeXray.backend.note.entity.NoteType;
import com.codeXray.backend.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;

    // 내 노트 목록(페이지). 태그는 Note.tags 의 @BatchSize 로 페이지 전체를 한 번에 읽는다
    public PageResponse<NoteResponse> findMyPage(Long userId, NoteType type, String search, Pageable pageable) {
        return PageResponse.from(noteRepository.search(userId, type, search, pageable).map(NoteResponse::from));
    }

    public NoteResponse findOne(Long id, Long userId) {
        return NoteResponse.from(getOwnedNote(id, userId));
    }

    @Transactional
    public NoteResponse create(Long userId, NoteType type, String title, String body,
                               String language, List<String> tags) {
        Note note = noteRepository.save(Note.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .language(language)
                .tags(tags)
                .build());
        return NoteResponse.from(note);
    }

    @Transactional
    public NoteResponse update(Long id, Long userId, NoteType type, String title, String body,
                               String language, List<String> tags) {
        Note note = getOwnedNote(id, userId);
        note.update(type, title, body, language, tags); // dirty checking
        return NoteResponse.from(note);
    }

    @Transactional
    public void remove(Long id, Long userId) {
        Note note = getOwnedNote(id, userId);
        noteRepository.delete(note);
    }

    // 404(없음) → 403(남의 것) 순서로 검사. SolutionService.getOwnedSolution 과 같은 방식
    private Note getOwnedNote(Long id, Long userId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return note;
    }
}
