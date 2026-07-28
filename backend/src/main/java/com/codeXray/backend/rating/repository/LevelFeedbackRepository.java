package com.codeXray.backend.rating.repository;

import com.codeXray.backend.rating.entity.LevelFeedback;
import com.codeXray.backend.stats.dto.StatsProjections.WeakTagCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LevelFeedbackRepository extends JpaRepository<LevelFeedback, Long> {

    // 내 피드백 조회용(upsert)
    Optional<LevelFeedback> findByUserIdAndProblemId(Long userId, Long problemId);

    // 특정 문제 재계산 시 그 문제에 달린 모든 피드백 레벨 수집
    List<LevelFeedback> findByProblemId(Long problemId);

    // 약점 태그: 내가 "어렵다"고 느낀(체감 난이도 평균 높은) 문제들의 태그.
    // LevelFeedback 은 problemId(raw) 라 Problem 을 on 절로 엔티티 조인.
    @Query("select t.name as tag, avg(lf.level) as avgLevel, count(lf) as count " +
            "from LevelFeedback lf join Problem p on p.id = lf.problemId " +
            "join p.problemTags pt join pt.tag t " +
            "where lf.userId = :userId " +
            "group by t.name having avg(lf.level) >= 3 order by avg(lf.level) desc")
    List<WeakTagCount> weakTags(@Param("userId") Long userId, Pageable pageable);
}
