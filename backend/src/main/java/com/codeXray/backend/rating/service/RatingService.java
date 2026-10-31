package com.codeXray.backend.rating.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.config.CacheConfig;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.repository.ProblemRepository;
import com.codeXray.backend.rating.dto.FeedbackResponse;
import com.codeXray.backend.rating.entity.LevelFeedback;
import com.codeXray.backend.rating.repository.LevelFeedbackRepository;
import com.codeXray.backend.rating.util.RatingCalculator;
import com.codeXray.backend.solution.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
    private final SolutionRepository solutionRepository;

    // 피드백 제출(upsert) → 즉시 그 문제의 adjustedLevel/tier 재계산
    // 이 문제의 adjustedLevel/tier가 바뀌므로 관련 캐시를 무효화:
    //  - 상세: 해당 id만 정밀 evict
    //  - 목록: 어떤 필터/페이지 키가 이 문제를 담고 있는지 특정 불가 → 전체 evict(allEntries)
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROBLEM_DETAIL, key = "#problemId"),
            @CacheEvict(cacheNames = CacheConfig.PROBLEM_LIST, allEntries = true)
    })
    @Transactional
    public FeedbackResponse submitFeedback(Long userId, Long problemId, int level) {
        /*
         * 맨 먼저 문제 행을 잠근다(없으면 404). 잠금은 이 트랜잭션이 끝날 때 풀린다.
         * 예전에는 잠금 없이 "피드백 저장 → 전체 피드백 읽기 → 계산 → 덮어쓰기"를 해서, 두 사람이 동시에 제출하면
         * 둘 다 상대의 (아직 커밋 안 된) 피드백을 못 본 채 계산하고 나중에 커밋한 쪽이 이겨 피드백 하나가 빠진 티어가 남았다.
         * 이제 같은 문제의 제출은 줄을 서고, 뒤에 선 쪽은 앞사람 커밋 뒤의 피드백 전체로 계산한다.
         * 문제마다 잠금이라 다른 문제의 제출과는 서로 기다리지 않는다
         */
        Problem problem = problemRepository.findByIdForUpdate(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        // 실제로 풀어본(=풀이를 등록한) 문제만 체감 난이도를 남길 수 있음
        if (solutionRepository.findByUserIdAndProblemId(userId, problemId).isEmpty()) {
            throw new BusinessException(ErrorCode.FEEDBACK_REQUIRES_SOLUTION);
        }

        LevelFeedback feedback = feedbackRepository.findByUserIdAndProblemId(userId, problemId)
                .map(existing -> { existing.updateLevel(level); return existing; })
                .orElseGet(() -> feedbackRepository.save(
                        LevelFeedback.builder().userId(userId).problemId(problemId).level(level).build()));

        feedbackRepository.flush(); // 방금 저장한 피드백이 아래 조회에 들어가도록 먼저 DB 로 보낸다
        applyRecomputed(problem);
        return FeedbackResponse.from(feedback);
    }

    // 특정 문제에 대한 내 피드백 (없으면 null = 미제출)
    public FeedbackResponse getMyFeedback(Long userId, Long problemId) {
        return feedbackRepository.findByUserIdAndProblemId(userId, problemId)
                .map(FeedbackResponse::from)
                .orElse(null);
    }

    // 단일 문제 재계산: 원본레벨 + 정답률 + 모든 피드백 → adjustedLevel/tier
    // 잠근 문제의 피드백 전체로 adjustedLevel/tier 를 다시 계산해 반영(dirty checking 으로 UPDATE)
    private void applyRecomputed(Problem problem) {
        List<Integer> levels = feedbackRepository.findByProblemId(problem.getId()).stream()
                .map(LevelFeedback::getLevel)
                .toList();

        double adjusted = RatingCalculator.computeAdjustedLevel(
                problem.getLevel(), problem.getAcceptanceRate(), levels);
        problem.applyRating(adjusted, RatingCalculator.levelToTier(adjusted));
    }

    // 전체 재계산(배치용). 피드백을 문제별로 미리 그룹핑해 N+1 회피.
    // 모든 문제의 tier가 바뀔 수 있으므로 문제 관련 캐시를 통째로 비운다.
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROBLEM_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROBLEM_LIST, allEntries = true)
    })
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
