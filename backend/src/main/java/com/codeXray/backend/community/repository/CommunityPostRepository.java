package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.CommunityPost;
import com.codeXray.backend.community.entity.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>, CommunityPostRepositoryCustom {

    // 미처리 건의사항(상태 미지정 + 숨김 아님 + 오래된) — 스케줄러 다이제스트용
    @Query("select p from CommunityPost p " +
            "where p.type in :types and p.status is null and p.hidden = false and p.createdAt < :cutoff " +
            "order by p.createdAt asc")
    List<CommunityPost> findStaleSuggestions(@Param("types") List<PostType> types,
                                             @Param("cutoff") LocalDateTime cutoff);
}
