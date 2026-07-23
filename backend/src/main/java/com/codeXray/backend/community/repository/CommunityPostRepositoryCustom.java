package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.entity.SuggestionStatus;

import java.util.List;

public interface CommunityPostRepositoryCustom {

    // 가시성(비공개/숨김) + 필터를 적용한 게시글 목록. userId=null 이면 비로그인.
    List<CommunityPost> findVisiblePosts(Long userId, boolean isAdmin,
                                         List<PostType> types, Long problemId,
                                         SuggestionStatus status, Long authorId);
}
