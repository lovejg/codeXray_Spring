package com.codeXray.backend.community.dto;

import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;

// 신고 목록에서 보여줄 대상 글 요약
public record ReportedPostResponse(Long id, String title, PostType type, boolean hidden, AuthorResponse author) {

    public static ReportedPostResponse from(CommunityPost p) {
        return new ReportedPostResponse(p.getId(), p.getTitle(), p.getType(), p.isHidden(), AuthorResponse.from(p.getUser()));
    }
}
