package com.codeXray.backend.community.dto;

import com.codeXray.backend.problem.entity.Problem;

// 게시글에 연결된 문제 요약(선택). 없으면 null.
public record ProblemBriefResponse(Long id, String title) {

    public static ProblemBriefResponse from(Problem problem) {
        return (problem == null) ? null : new ProblemBriefResponse(problem.getId(), problem.getTitle());
    }
}
