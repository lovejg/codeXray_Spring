package com.codeXray.backend.community.dto;

import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.entity.SuggestionStatus;

import java.time.LocalDateTime;

// 목록 아이템 (본문 content 제외, 댓글 수 + 투표 집계 포함).
public record PostSummaryResponse(
        Long id,
        AuthorResponse author,
        ProblemBriefResponse problem,
        PostType type,
        String title,
        boolean isPrivate,
        boolean hidden,
        SuggestionStatus status,
        long commentCount,
        VoteSummary votes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostSummaryResponse from(CommunityPost p, long commentCount, VoteSummary votes) {
        return new PostSummaryResponse(
                p.getId(),
                AuthorResponse.from(p.getUser()),
                ProblemBriefResponse.from(p.getProblem()),
                p.getType(),
                p.getTitle(),
                p.isPrivate(),
                p.isHidden(),
                p.getStatus(),
                commentCount,
                votes,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
