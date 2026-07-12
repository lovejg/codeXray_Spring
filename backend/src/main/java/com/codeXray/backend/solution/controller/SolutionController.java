package com.codeXray.backend.solution.controller;

import com.codeXray.backend.solution.dto.CreateSolutionRequest;
import com.codeXray.backend.solution.dto.MemoResponse;
import com.codeXray.backend.solution.dto.SolutionResponse;
import com.codeXray.backend.solution.dto.UpdateSolutionRequest;
import com.codeXray.backend.solution.dto.UpsertMemoRequest;
import com.codeXray.backend.solution.service.SolutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solutions")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;

    // GET /api/solutions?starred=true  — 내 풀이 목록 (starred 생략 시 전체)
    @GetMapping
    public List<SolutionResponse> findMyAll(@AuthenticationPrincipal Long userId,
                                            @RequestParam(required = false) Boolean starred) {
        return solutionService.findMyAll(userId, starred);
    }

    // GET /api/solutions/{id} — 내 풀이 단건 (메모 포함)
    @GetMapping("/{id}")
    public SolutionResponse findOne(@PathVariable Long id,
                                    @AuthenticationPrincipal Long userId) {
        return solutionService.findOne(id, userId);
    }

    // POST /api/solutions — 풀이 등록(upsert)
    @PostMapping
    public ResponseEntity<SolutionResponse> create(@AuthenticationPrincipal Long userId,
                                                   @RequestBody @Valid CreateSolutionRequest req) {
        SolutionResponse body = solutionService.create(userId, req.problemId(), req.code(), req.language());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // PUT /api/solutions/{id} — 풀이 수정
    @PutMapping("/{id}")
    public SolutionResponse update(@PathVariable Long id,
                                   @AuthenticationPrincipal Long userId,
                                   @RequestBody @Valid UpdateSolutionRequest req) {
        return solutionService.update(id, userId, req.code(), req.language());
    }

    // PATCH /api/solutions/{id}/star — "다시 풀 문제" 토글
    @PatchMapping("/{id}/star")
    public SolutionResponse toggleStar(@PathVariable Long id,
                                       @AuthenticationPrincipal Long userId) {
        return solutionService.toggleStar(id, userId);
    }

    // PUT /api/solutions/{id}/memo — 메모 upsert
    @PutMapping("/{id}/memo")
    public MemoResponse upsertMemo(@PathVariable Long id,
                                   @AuthenticationPrincipal Long userId,
                                   @RequestBody UpsertMemoRequest req) {
        return solutionService.upsertMemo(id, userId,
                req.wrongReason(), req.logic(), req.keyFunctions(), req.freeNote());
    }

    // DELETE /api/solutions/{id} — 풀이 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id,
                                       @AuthenticationPrincipal Long userId) {
        solutionService.remove(id, userId);
        return ResponseEntity.noContent().build();
    }
}
