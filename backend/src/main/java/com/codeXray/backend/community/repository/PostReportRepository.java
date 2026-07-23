package com.codeXray.backend.community.repository;

import com.codeXray.backend.community.entity.PostReport;
import com.codeXray.backend.community.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    // 중복 신고 방지 (unique 제약과 함께 선제 검사)
    boolean existsByReporterIdAndPostId(Long userId, Long postId);

    // 관리자 신고 목록 (status 필터 유무)
    List<PostReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<PostReport> findAllByOrderByStatusAscCreatedAtDesc();

    // 숨김 처리 시: 이 글의 OPEN 신고자들(알림 대상) → 그다음 일괄 상태 변경
    @Query("select distinct r.reporter.id from PostReport r where r.post.id = :postId and r.status = :status")
    List<Long> findReporterIds(@Param("postId") Long postId, @Param("status") ReportStatus status);

    @Modifying
    @Query("update PostReport r set r.status = :to where r.post.id = :postId and r.status = :from")
    int bulkUpdateStatus(@Param("postId") Long postId,
                         @Param("from") ReportStatus from,
                         @Param("to") ReportStatus to);
}
