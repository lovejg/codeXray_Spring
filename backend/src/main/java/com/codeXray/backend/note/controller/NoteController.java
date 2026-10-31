package com.codeXray.backend.note.controller;

import com.codeXray.backend.common.dto.PageRequests;
import com.codeXray.backend.common.dto.PageResponse;
import com.codeXray.backend.note.dto.CreateNoteRequest;
import com.codeXray.backend.note.dto.NoteResponse;
import com.codeXray.backend.note.dto.UpdateNoteRequest;
import com.codeXray.backend.note.entity.NoteType;
import com.codeXray.backend.note.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    // 내 노트 목록 (type / search 필터, page/size 페이지 — size 최대 50)
    @GetMapping
    public PageResponse<NoteResponse> findMyPage(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) NoteType type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return noteService.findMyPage(userId, type, search, PageRequests.of(page, size));
    }

    @GetMapping("/{id}")
    public NoteResponse findOne(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId
    ) {
        return noteService.findOne(id, userId);
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateNoteRequest req
    ) {
        NoteResponse res = noteService.create(
                userId, req.type(), req.title(), req.body(), req.language(), req.tags());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    public NoteResponse update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNoteRequest req
    ) {
        return noteService.update(
                id, userId, req.type(), req.title(), req.body(), req.language(), req.tags());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId
    ) {
        noteService.remove(id, userId);
        return ResponseEntity.noContent().build();
    }
}
