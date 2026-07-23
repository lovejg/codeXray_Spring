package com.codeXray.backend.community.entity;

// 건의사항 계열(FEEDBACK/BUG_REPORT/FEATURE_REQUEST)에만 사용. 커뮤니티 글은 null.
public enum SuggestionStatus {
    IN_PROGRESS, // 처리 중
    RESOLVED     // 해결됨
}
