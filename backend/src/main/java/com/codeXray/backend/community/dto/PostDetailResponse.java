package com.codeXray.backend.community.dto;

import com.codeXray.backend.community.entity.Comment;
import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.entity.SuggestionStatus;

import java.time.LocalDateTime;
import java.util.List;

// 상세 (본문 + 관리자답변 + 댓글 목록)
public record PostDetailResponse(
        Long id,
        AuthorResponse author,
        ProblemBriefResponse problem,
        PostType type,
        String title,
        String content,
        boolean isPrivate,
        boolean hidden,
        SuggestionStatus status,
        String adminReply,
        LocalDateTime adminReplyAt,
        VoteSummary votes,
        List<CommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse from(CommunityPost p, List<Comment> comments, VoteSummary votes) {
        return new PostDetailResponse(
                p.getId(),
                AuthorResponse.from(p.getUser()),
                ProblemBriefResponse.from(p.getProblem()),
                p.getType(),
                p.getTitle(),
                p.getContent(),
                p.isPrivate(),
                p.isHidden(),
                p.getStatus(),
                p.getAdminReply(),
                p.getAdminReplyAt(),
                votes,
                comments.stream().map(CommentResponse::from).toList(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
