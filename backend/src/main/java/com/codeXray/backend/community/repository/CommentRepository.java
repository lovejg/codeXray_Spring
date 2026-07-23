package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 상세 조회 시 댓글(오래된 순)
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // 목록의 게시글별 댓글 수를 한 방에 집계 (N+1 회피). row = [postId, count]
    @Query("select c.post.id, count(c) from Comment c where c.post.id in :postIds group by c.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);
}
