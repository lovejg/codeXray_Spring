package com.codeXray.backend.rating.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.repository.ProblemRepository;
import com.codeXray.backend.rating.dto.FeedbackResponse;
import com.codeXray.backend.rating.entity.LevelFeedback;
import com.codeXray.backend.rating.repository.LevelFeedbackRepository;
import com.codeXray.backend.rating.util.RatingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService {

    private final LevelFeedbackRepository feedbackRepository;
    private final ProblemRepository problemRepository;

    // 피드백 제출(upsert) → 즉시 그 문제의 adjustedLevel/tier 재계산
    @Transactional
    public FeedbackResponse submitFeedback(Long userId, Long problemId, int level) {
        // 존재하지 않는 문제에 피드백 남기는 것 차단 (save 전에 걸러 깔끔한 404)
        if (!problemRepository.existsById(problemId)) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        LevelFeedback feedback = feedbackRepository.findByUserIdAndProblemId(userId, problemId)
                .map(existing -> { existing.updateLevel(level); return existing; })
                .orElseGet(() -> feedbackRepository.save(
                        LevelFeedback.builder().userId(userId).problemId(problemId).level(level).build()));

        recomputeProblem(problemId);
        return FeedbackResponse.from(feedback);
    }

    // 특정 문제에 대한 내 피드백 (없으면 null = 미제출)
    public FeedbackResponse getMyFeedback(Long userId, Long problemId) {
        return feedbackRepository.findByUserIdAndProblemId(userId, problemId)
                .map(FeedbackResponse::from)
                .orElse(null);
    }

    // 단일 문제 재계산: 원본레벨 + 정답률 + 모든 피드백 → adjustedLevel/tier
    @Transactional
    public void recomputeProblem(Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        List<Integer> levels = feedbackRepository.findByProblemId(problemId).stream()
                .map(LevelFeedback::getLevel)
                .toList();

        double adjusted = RatingCalculator.computeAdjustedLevel(
                problem.getLevel(), problem.getAcceptanceRate(), levels);
        problem.applyRating(adjusted, RatingCalculator.levelToTier(adjusted));
    }

    // 전체 재계산(배치용). 피드백을 문제별로 미리 그룹핑해 N+1 회피.
    @Transactional
    public int recomputeAll() {
        List<Problem> problems = problemRepository.findAll();

        Map<Long, List<Integer>> levelsByProblem = feedbackRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        LevelFeedback::getProblemId,
                        Collectors.mapping(LevelFeedback::getLevel, Collectors.toList())));

        for (Problem p : problems) {
            List<Integer> levels = levelsByProblem.getOrDefault(p.getId(), List.of());
            double adjusted = RatingCalculator.computeAdjustedLevel(
                    p.getLevel(), p.getAcceptanceRate(), levels);
            p.applyRating(adjusted, RatingCalculator.levelToTier(adjusted));
        }
        return problems.size();
    }
}
