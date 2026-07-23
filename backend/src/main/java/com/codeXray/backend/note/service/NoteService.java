package com.codeXray.backend.note.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.note.dto.NoteResponse;
import com.codeXray.backend.note.entity.Note;
import com.codeXray.backend.note.entity.NoteType;
import com.codeXray.backend.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;

    public List<NoteResponse> findMyAll(Long userId, NoteType type, String search) {
        return noteRepository.search(userId, type, search).stream()
                .map(NoteResponse::from)
                .toList();
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

    // 404(없음) → 403(남의 것) 순서로 검사. Stage 5 getOwnedSolution 과 동일 패턴.
    private Note getOwnedNote(Long id, Long userId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return note;
    }
}
