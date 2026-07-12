package com.codeXray.backend.solution.dto;

import com.codeXray.backend.problem.dto.ProblemResponse;
import com.codeXray.backend.solution.entity.Solution;

import java.time.LocalDateTime;

public record SolutionResponse(
        Long id,
        ProblemResponse problem,
        String code,
        String language,
        boolean starred,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        MemoResponse memo
) {
    public static SolutionResponse from(Solution solution) {
        MemoResponse memo = (solution.getMemo() == null)
                ? null
                : MemoResponse.from(solution.getMemo());

        return new SolutionResponse(
                solution.getId(),
                ProblemResponse.from(solution.getProblem()),
                solution.getCode(),
                solution.getLanguage(),
                solution.isStarred(),
                solution.getCreatedAt(),
                solution.getUpdatedAt(),
                memo
        );
    }
}
