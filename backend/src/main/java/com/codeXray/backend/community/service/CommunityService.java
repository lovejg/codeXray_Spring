package com.codeXray.backend.community.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.community.dto.*;
import com.codeXray.backend.community.entity.*;
import com.codeXray.backend.community.repository.CommentRepository;
import com.codeXray.backend.community.repository.CommunityPostRepository;
import com.codeXray.backend.community.repository.PostReportRepository;
import com.codeXray.backend.community.repository.PostVoteRepository;
import com.codeXray.backend.notification.entity.NotificationType;
import com.codeXray.backend.notification.service.NotificationService;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.repository.ProblemRepository;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final CommunityPostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostVoteRepository postVoteRepository;
    private final PostReportRepository postReportRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final NotificationService notificationService;

    private static final Set<PostType> VOTABLE = Set.of(PostType.QUESTION, PostType.SOLUTION_SHARE);
    private static final Set<PostType> SUGGESTION = Set.of(PostType.FEEDBACK, PostType.BUG_REPORT, PostType.FEATURE_REQUEST);

    // ─── 게시글 ───────────────────────────────────────────
    public List<PostSummaryResponse> findAllPosts(Long userId, boolean isAdmin, List<PostType> types,
                                                  Long problemId, SuggestionStatus status, Long authorId,
                                                  String sort) {
        List<CommunityPost> posts = postRepository.findVisiblePosts(userId, isAdmin, types, problemId, status, authorId);
        if (posts.isEmpty()) return List.of();

        List<Long> ids = posts.stream().map(CommunityPost::getId).toList();

        Map<Long, Long> commentCounts = new HashMap<>();
        for (Object[] row : commentRepository.countByPostIds(ids)) {
            commentCounts.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, VoteSummary> votes = aggregateVotes(ids, userId);

        List<PostSummaryResponse> result = new ArrayList<>();
        for (CommunityPost p : posts) {
            result.add(PostSummaryResponse.from(
                    p,
                    commentCounts.getOrDefault(p.getId(), 0L),
                    votes.getOrDefault(p.getId(), VoteSummary.empty())));
        }

        if ("votes".equalsIgnoreCase(sort)) {
            result.sort(Comparator
                    .comparingInt((PostSummaryResponse r) -> r.votes().score()).reversed()
                    .thenComparing(PostSummaryResponse::createdAt, Comparator.reverseOrder()));
        }
        return result;
    }

    public PostDetailResponse findOnePost(Long id, Long userId, boolean isAdmin) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        boolean owner = isOwner(post, userId);
        if (post.isPrivate() && !owner && !isAdmin) throw new BusinessException(ErrorCode.PRIVATE_POST);
        if (post.isHidden() && !owner && !isAdmin) throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        return toDetail(post, userId);
    }

    @Transactional
    public PostDetailResponse createPost(Long userId, Long problemId, PostType type,
                                         String title, String content, Boolean isPrivate) {
        User author = userRepository.getReferenceById(userId);
        Problem problem = (problemId == null) ? null
                : problemRepository.findById(problemId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        CommunityPost post = postRepository.save(CommunityPost.builder()
                .user(author)
                .problem(problem)
                .type(type)
                .title(title)
                .content(content)
                .isPrivate(isPrivate != null && isPrivate)
                .build());
        return PostDetailResponse.from(post, List.of(), VoteSummary.empty());
    }

    @Transactional
    public PostDetailResponse updatePost(Long id, Long userId, String title, String content, Boolean isPrivate) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (!isOwner(post, userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        post.update(title, content, isPrivate);
        return toDetail(post, userId);
    }

    @Transactional
    public void deletePost(Long id, Long userId, boolean isAdmin) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (!isOwner(post, userId) && !isAdmin) throw new BusinessException(ErrorCode.FORBIDDEN);
        postRepository.delete(post); // 댓글/투표/신고는 DB CASCADE 로 정리
    }

    // ─── 댓글 ─────────────────────────────────────────────
    @Transactional
    public CommentResponse createComment(Long postId, Long userId, String content) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (post.isPrivate() && !isOwner(post, userId)) throw new BusinessException(ErrorCode.PRIVATE_POST);
        if (post.isHidden()) throw new BusinessException(ErrorCode.FORBIDDEN);

        User author = userRepository.getReferenceById(userId);
        Comment comment = commentRepository.save(Comment.builder()
                .post(post).user(author).content(content).build());

        // 글 작성자에게 댓글 알림 (본인이 본인 글에 단 경우 actorId==userId 로 스킵)
        notificationService.create(
                authorId(post),
                NotificationType.COMMENT,
                payload("postId", post.getId(), "postTitle", post.getTitle(),
                        "postType", post.getType().name(),
                        "commenterNickname", author.getNickname(),
                        "contentPreview", preview(content)),
                userId);

        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long id, Long userId, boolean isAdmin) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        boolean owner = comment.getUser() != null && comment.getUser().getId().equals(userId);
        if (!owner && !isAdmin) throw new BusinessException(ErrorCode.FORBIDDEN);
        commentRepository.delete(comment);
    }

    // ─── 투표 ─────────────────────────────────────────────
    @Transactional
    public VoteSummary vote(Long postId, Long userId, int value) {
        if (value != 1 && value != -1) throw new BusinessException(ErrorCode.INVALID_INPUT);

        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (!VOTABLE.contains(post.getType())) throw new BusinessException(ErrorCode.POST_NOT_VOTABLE);
        if (isOwner(post, userId)) throw new BusinessException(ErrorCode.CANNOT_VOTE_OWN_POST);
        if (post.isHidden()) throw new BusinessException(ErrorCode.FORBIDDEN);

        postVoteRepository.findByUserIdAndPostId(userId, postId)
                .map(existing -> { existing.updateValue(value); return existing; })
                .orElseGet(() -> postVoteRepository.save(
                        PostVote.builder().userId(userId).postId(postId).value(value).build()));

        return voteSummaryFor(postId, userId);
    }

    @Transactional
    public VoteSummary removeVote(Long postId, Long userId) {
        postVoteRepository.deleteByUserIdAndPostId(userId, postId);
        return voteSummaryFor(postId, userId);
    }

    // ─── 신고 ─────────────────────────────────────────────
    @Transactional
    public ReportResponse report(Long postId, Long userId, String reason) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (isOwner(post, userId)) throw new BusinessException(ErrorCode.CANNOT_REPORT_OWN_POST);
        if (postReportRepository.existsByReporterIdAndPostId(userId, postId)) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }

        User reporter = userRepository.getReferenceById(userId);
        PostReport report = postReportRepository.save(PostReport.builder()
                .reporter(reporter).post(post).reason(reason).build());

        // 모든 관리자에게 새 신고 알림 (신고자 본인이 관리자면 제외)
        notificationService.createForAllAdmins(
                NotificationType.NEW_REPORT,
                payload("reportId", report.getId(), "postId", post.getId(),
                        "postTitle", post.getTitle(), "reason", reason),
                userId);

        return ReportResponse.from(report);
    }

    public List<ReportResponse> listReports(ReportStatus status) {
        List<PostReport> reports = (status == null)
                ? postReportRepository.findAllByOrderByStatusAscCreatedAtDesc()
                : postReportRepository.findByStatusOrderByCreatedAtDesc(status);
        return reports.stream().map(ReportResponse::from).toList();
    }

    @Transactional
    public ReportResponse updateReport(Long id, ReportStatus status, String adminNote) {
        PostReport report = postReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        ReportStatus old = report.getStatus();
        report.resolve(status, adminNote);

        // 실제로 종결(OPEN→HANDLED/DISMISSED) 됐을 때만 신고자에게 알림
        if (old != status && (status == ReportStatus.HANDLED || status == ReportStatus.DISMISSED)) {
            notificationService.create(
                    report.getReporter().getId(),
                    NotificationType.REPORT_RESOLVED,
                    payload("postId", report.getPost().getId(), "postTitle", report.getPost().getTitle(),
                            "resolution", status.name(), "adminNote", adminNote));
        }
        return ReportResponse.from(report);
    }

    // ─── 관리자 숨김 (신고 일괄 처리 + 알림) ────────────────
    @Transactional
    public PostDetailResponse setHidden(Long postId, Long adminUserId, boolean hidden) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 알림에 쓸 값은 이후 bulk 연산 전에 미리 확보
        Long authorId = authorId(post);
        String title = post.getTitle();
        String typeName = post.getType().name();

        post.changeHidden(hidden);

        if (hidden) {
            // 숨길 때만: 이 글의 OPEN 신고자 수집 → OPEN 신고 일괄 HANDLED
            List<Long> reporterIds = postReportRepository.findReporterIds(postId, ReportStatus.OPEN);
            postReportRepository.bulkUpdateStatus(postId, ReportStatus.OPEN, ReportStatus.HANDLED);

            // 작성자에게 숨김 알림
            notificationService.create(authorId, NotificationType.POST_HIDDEN,
                    payload("postId", postId, "postTitle", title, "postType", typeName));

            // 신고자들에게 자동 처리 완료 알림
            for (Long reporterId : new HashSet<>(reporterIds)) {
                notificationService.create(reporterId, NotificationType.REPORT_RESOLVED,
                        payload("postId", postId, "postTitle", title,
                                "resolution", ReportStatus.HANDLED.name(), "autoResolved", true));
            }
        }
        return toDetail(post, adminUserId);
    }

    // ─── 건의사항 상태/답변 ────────────────────────────────
    @Transactional
    public PostDetailResponse updateStatus(Long id, Long adminUserId, SuggestionStatus status) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (!SUGGESTION.contains(post.getType())) throw new BusinessException(ErrorCode.NOT_SUGGESTION_POST);

        SuggestionStatus old = post.getStatus();
        post.changeStatus(status);

        if (old != status) {
            notificationService.create(authorId(post), NotificationType.STATUS_CHANGE,
                    payload("postId", post.getId(), "postTitle", post.getTitle(),
                            "oldStatus", old == null ? null : old.name(), "newStatus", status.name()));
        }
        return toDetail(post, adminUserId);
    }

    @Transactional
    public PostDetailResponse updateAdminReply(Long id, Long adminUserId, String adminReply) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (!SUGGESTION.contains(post.getType())) throw new BusinessException(ErrorCode.NOT_SUGGESTION_POST);

        String reply = (adminReply == null) ? "" : adminReply.trim();
        boolean hasReply = !reply.isEmpty();
        post.applyAdminReply(hasReply ? reply : null, hasReply ? LocalDateTime.now() : null);

        // 답변이 등록/수정된 경우만 알림 (제거는 알림 안 함)
        if (hasReply) {
            notificationService.create(authorId(post), NotificationType.ADMIN_REPLY,
                    payload("postId", post.getId(), "postTitle", post.getTitle(),
                            "replyPreview", preview(reply)));
        }
        return toDetail(post, adminUserId);
    }

    // ─── 투표 집계 헬퍼 ────────────────────────────────────
    private Map<Long, VoteSummary> aggregateVotes(List<Long> postIds, Long userId) {
        Map<Long, int[]> ud = new HashMap<>();
        for (Object[] r : postVoteRepository.aggregateByPostIds(postIds)) {
            Long pid = (Long) r[0];
            int val = (Integer) r[1];
            long cnt = (Long) r[2];
            int[] arr = ud.computeIfAbsent(pid, k -> new int[2]);
            if (val == 1) arr[0] = (int) cnt;
            else if (val == -1) arr[1] = (int) cnt;
        }

        Map<Long, Integer> mine = new HashMap<>();
        if (userId != null) {
            for (Object[] r : postVoteRepository.findMyVotes(userId, postIds)) {
                mine.put((Long) r[0], (Integer) r[1]);
            }
        }

        Map<Long, VoteSummary> result = new HashMap<>();
        for (Long pid : postIds) {
            int[] arr = ud.getOrDefault(pid, new int[2]);
            result.put(pid, VoteSummary.of(arr[0], arr[1], mine.getOrDefault(pid, 0)));
        }
        return result;
    }

    private VoteSummary voteSummaryFor(Long postId, Long userId) {
        return aggregateVotes(List.of(postId), userId).getOrDefault(postId, VoteSummary.empty());
    }

    // ─── 공통 헬퍼 ─────────────────────────────────────────
    private PostDetailResponse toDetail(CommunityPost post, Long userId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId());
        return PostDetailResponse.from(post, comments, voteSummaryFor(post.getId(), userId));
    }

    private boolean isOwner(CommunityPost post, Long userId) {
        return userId != null && post.getUser() != null && post.getUser().getId().equals(userId);
    }

    private Long authorId(CommunityPost post) {
        return post.getUser() == null ? null : post.getUser().getId();
    }

    private static String preview(String s) {
        return s.length() <= 80 ? s : s.substring(0, 80);
    }

    // null 값도 허용하는 payload 맵 (Map.of 는 null 금지 → jsonb 에 null 담을 때 사용)
    private static Map<String, Object> payload(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
