package com.codeXray.backend.community.entity;

import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자 표시(닉네임)가 필요해 @ManyToOne. nullable = 탈퇴 시 FK가 SET NULL → 글은 보존, 작성자만 익명화.
    // (Solution/Note 는 id 비교만 해서 raw Long 이었지만, 여기선 연관 엔티티를 화면에 노출하므로 객체 매핑.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 문제 연결(선택). 문제 삭제 시 SET NULL.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate; // true 면 작성자 + 관리자만 조회

    @Column(nullable = false)
    private boolean hidden; // 관리자가 신고 처리로 숨김

    @Enumerated(EnumType.STRING)
    private SuggestionStatus status; // 건의사항 계열만 사용

    @Column(columnDefinition = "TEXT")
    private String adminReply; // 관리자 공식 답변

    private LocalDateTime adminReplyAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public CommunityPost(User user, Problem problem, PostType type, String title,
                         String content, boolean isPrivate) {
        this.user = user;
        this.problem = problem;
        this.type = type;
        this.title = title;
        this.content = content;
        this.isPrivate = isPrivate;
        this.hidden = false;
    }

    // 작성자 수정(제목/본문/공개여부). isPrivate 가 null 이면 기존값 유지.
    public void update(String title, String content, Boolean isPrivate) {
        this.title = title;
        this.content = content;
        if (isPrivate != null) this.isPrivate = isPrivate;
    }

    // ── 관리자 운영 ──
    public void changeHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void changeStatus(SuggestionStatus status) {
        this.status = status;
    }

    // 답변 등록/수정(빈 값이면 reply=null, at=null 로 제거)
    public void applyAdminReply(String adminReply, LocalDateTime adminReplyAt) {
        this.adminReply = adminReply;
        this.adminReplyAt = adminReplyAt;
    }
}
