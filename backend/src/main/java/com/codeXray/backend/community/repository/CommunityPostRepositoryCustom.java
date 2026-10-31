package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import com.codeXray.backend.community.entity.SuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommunityPostRepositoryCustom {

    // 가시성(비공개/숨김) + 필터를 적용한 게시글 목록(페이지). userId=null 이면 비로그인.
    // byVotes=true 면 추천 점수 높은 순, 아니면 최신순
    Page<CommunityPost> findVisiblePosts(Long userId, boolean isAdmin,
                                         List<PostType> types, Long problemId,
                                         SuggestionStatus status, Long authorId,
                                         boolean byVotes, Pageable pageable);
}
