package com.codeXray.backend.community.dto;

import com.codeXray.backend.community.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(Long id, AuthorResponse author, String content, LocalDateTime createdAt) {

    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), AuthorResponse.from(c.getUser()), c.getContent(), c.getCreatedAt());
    }
}
