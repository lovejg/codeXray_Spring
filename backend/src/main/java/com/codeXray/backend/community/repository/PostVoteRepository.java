package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.PostVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostVoteRepository extends JpaRepository<PostVote, Long> {

    // upsert 프로브
    Optional<PostVote> findByUserIdAndPostId(Long userId, Long postId);

    // 투표 철회 (없으면 아무 일도 안 함)
    void deleteByUserIdAndPostId(Long userId, Long postId);

    // 여러 게시글의 추천/비추천 수를 한 번에 집계. row = [postId, value, count]
    @Query("select v.postId, v.value, count(v) from PostVote v where v.postId in :postIds group by v.postId, v.value")
    List<Object[]> aggregateByPostIds(@Param("postIds") List<Long> postIds);

    // 내가 이 게시글들에 던진 표. row = [postId, value]
    @Query("select v.postId, v.value from PostVote v where v.userId = :userId and v.postId in :postIds")
    List<Object[]> findMyVotes(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
