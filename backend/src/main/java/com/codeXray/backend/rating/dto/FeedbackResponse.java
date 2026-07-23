package com.codeXray.backend.rating.dto;

import com.codeXray.backend.rating.entity.LevelFeedback;

// 내 피드백 조회/제출 응답. 없으면 컨트롤러에서 null(=미제출)로 응답
public record FeedbackResponse(Long problemId, int level) {
    public static FeedbackResponse from(LevelFeedback f) {
        return new FeedbackResponse(f.getProblemId(), f.getLevel());
    }
}
