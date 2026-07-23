package com.codeXray.backend.community.entity;

import com.codeXray.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 게시글 신고. 목록에서 신고자 닉네임/글 정보를 보여줘야 해 연관 매핑. 둘 다 CASCADE(유저/글 삭제 시 신고도 정리).
@Entity
@Table(
        name = "post_reports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}) // 중복 신고 방지
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    private String adminNote;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public PostReport(User reporter, CommunityPost post, String reason) {
        this.reporter = reporter;
        this.post = post;
        this.reason = reason;
        this.status = ReportStatus.OPEN;
    }

    // 관리자 처리: 상태 + 메모 갱신
    public void resolve(ReportStatus status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote;
    }
}
