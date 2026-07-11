package com.codeXray.backend.problem.dto;

import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.entity.ProblemSource;
import com.codeXray.backend.problem.entity.Tier;

import java.util.List;

public record ProblemResponse(
        Long id,
        String title,
        ProblemSource source,
        int level,
        Double acceptanceRate,
        Double adjustedLevel,
        Tier tier,
        String link,
        List<TagResponse> tags
) {
    public static ProblemResponse from(Problem problem) {
        // 이 문제의 연결(ProblemTag)들 → 각 태그 → TagResponse 로 변환
        List<TagResponse> tags = problem.getProblemTags().stream()
                .map(pt -> TagResponse.from(pt.getTag()))
                .toList();

        return new ProblemResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getSource(),
                problem.getLevel(),
                problem.getAcceptanceRate(),
                problem.getAdjustedLevel(),
                problem.getTier(),
                problem.getLink(),
                tags
        );
    }
}
