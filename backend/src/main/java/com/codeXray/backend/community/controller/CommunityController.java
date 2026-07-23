package com.codeXray.backend.community.controller;

import com.codeXray.backend.community.dto.*;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.entity.ReportStatus;
import com.codeXray.backend.community.entity.SuggestionStatus;
import com.codeXray.backend.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // 목록: 비로그인도 조회 가능(공개글만). 토큰 있으면 내 비공개글/숨김글도 보임.
    @GetMapping("/posts")
    public List<PostSummaryResponse> findAll(
            Authentication auth,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) String types,   // CSV
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) SuggestionStatus status,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String sort   // recent(기본) | votes
    ) {
        return communityService.findAllPosts(
                userId(auth), isAdmin(auth), parseTypes(type, types), problemId, status, authorId, sort);
    }

    @GetMapping("/posts/{id}")
    public PostDetailResponse findOne(@PathVariable Long id, Authentication auth) {
        return communityService.findOnePost(id, userId(auth), isAdmin(auth));
    }

    @PostMapping("/posts")
    public ResponseEntity<PostDetailResponse> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePostRequest req
    ) {
        PostDetailResponse res = communityService.createPost(
                userId, req.problemId(), req.type(), req.title(), req.content(), req.isPrivate());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/posts/{id}")
    public PostDetailResponse updatePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdatePostRequest req
    ) {
        return communityService.updatePost(id, userId, req.title(), req.content(), req.isPrivate());
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, Authentication auth) {
        communityService.deletePost(id, userId(auth), isAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    // ─── 신고 ─────────────────────────────────────────────
    @PostMapping("/posts/{id}/report")
    public ResponseEntity<ReportResponse> report(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateReportRequest req
    ) {
        ReportResponse res = communityService.report(id, userId, req.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // ─── 관리자 전용 (SecurityConfig 에서 ROLE_ADMIN 강제) ───
    @GetMapping("/admin/reports")
    public List<ReportResponse> listReports(@RequestParam(required = false) ReportStatus status) {
        return communityService.listReports(status);
    }

    @PatchMapping("/admin/reports/{id}")
    public ReportResponse updateReport(@PathVariable Long id, @Valid @RequestBody UpdateReportRequest req) {
        return communityService.updateReport(id, req.status(), req.adminNote());
    }

    @PatchMapping("/admin/posts/{id}/hide")
    public PostDetailResponse hidePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody HidePostRequest req
    ) {
        return communityService.setHidden(id, userId, req.hidden());
    }

    @PatchMapping("/posts/{id}/status")
    public PostDetailResponse updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateStatusRequest req
    ) {
        return communityService.updateStatus(id, userId, req.status());
    }

    @PatchMapping("/posts/{id}/admin-reply")
    public PostDetailResponse updateAdminReply(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateAdminReplyRequest req
    ) {
        return communityService.updateAdminReply(id, userId, req.adminReply());
    }

    // ─── 투표 ─────────────────────────────────────────────
    @PostMapping("/posts/{id}/vote")
    public VoteSummary vote(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VotePostRequest req
    ) {
        return communityService.vote(id, userId, req.value());
    }

    @DeleteMapping("/posts/{id}/vote")
    public VoteSummary removeVote(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return communityService.removeVote(id, userId);
    }

    // ─── 댓글 ─────────────────────────────────────────────
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateCommentRequest req
    ) {
        CommentResponse res = communityService.createComment(id, userId, req.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, Authentication auth) {
        communityService.deleteComment(id, userId(auth), isAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    // ─── 현재 사용자 추출 (선택적 인증) ──────────────────────
    // 비로그인이면 principal 이 Long 이 아니므로 null. 토큰 있으면 JwtAuthenticationFilter 가 심은 userId.
    private static Long userId(Authentication auth) {
        return (auth != null && auth.getPrincipal() instanceof Long id) ? id : null;
    }

    private static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ?type=QUESTION 단일 또는 ?types=QUESTION,SOLUTION_SHARE CSV. 알 수 없는 값은 무시.
    private static List<PostType> parseTypes(PostType type, String typesCsv) {
        if (typesCsv != null && !typesCsv.isBlank()) {
            List<PostType> list = new ArrayList<>();
            for (String token : typesCsv.split(",")) {
                String t = token.trim();
                if (t.isEmpty()) continue;
                try {
                    list.add(PostType.valueOf(t));
                } catch (IllegalArgumentException ignored) {
                    // 잘못된 타입 토큰은 건너뜀
                }
            }
            return list;
        }
        return (type != null) ? List.of(type) : List.of();
    }
}
