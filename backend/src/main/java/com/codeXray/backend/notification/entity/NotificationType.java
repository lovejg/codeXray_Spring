package com.codeXray.backend.notification.entity;

public enum NotificationType {
    COMMENT,          // 내 글에 댓글
    ADMIN_REPLY,      // 내 건의사항에 관리자 답변
    STATUS_CHANGE,    // 내 건의사항 상태 변경
    POST_HIDDEN,      // 내 글 숨김 처리됨
    REPORT_RESOLVED,  // 내가 한 신고 처리 완료/기각
    NEW_REPORT,       // (관리자) 새 신고 도착
    TIER_UP,          // 새 티어 패밀리 첫 해결
    STALE_SUGGESTION  // (관리자) 7일 이상 미처리 건의사항 다이제스트
}
