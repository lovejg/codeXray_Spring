package com.codeXray.backend.solution.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.notification.entity.NotificationType;
import com.codeXray.backend.notification.service.NotificationService;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.entity.Tier;
import com.codeXray.backend.problem.repository.ProblemRepository;
import com.codeXray.backend.solution.dto.MemoResponse;
import com.codeXray.backend.solution.dto.SolutionResponse;
import com.codeXray.backend.solution.entity.Memo;
import com.codeXray.backend.solution.entity.Solution;
import com.codeXray.backend.solution.repository.MemoRepository;
import com.codeXray.backend.solution.repository.SolutionRepository;
import com.codeXray.backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionService {

    private final SolutionRepository solutionRepository;
    private final MemoRepository memoRepository;
    private final ProblemRepository problemRepository;
    private final NotificationService notificationService;

    // 내 풀이 목록(페이지). starred 가 null 이면 전체
    public PageResponse<SolutionResponse> findMyPage(Long userId, Boolean starred, Pageable pageable) {
        Page<Solution> page = (starred == null)
                ? solutionRepository.findPageByUserId(userId, pageable)
                : solutionRepository.findPageByUserIdAndStarred(userId, starred, pageable);
        return PageResponse.from(page.map(SolutionResponse::from));
    }

    // 이 문제에 대한 내 풀이. 없으면 빈 값(아직 안 푼 문제)
    public Optional<SolutionResponse> findMineByProblem(Long userId, Long problemId) {
        return solutionRepository.findDetailedByUserIdAndProblemId(userId, problemId).map(SolutionResponse::from);
    }

    // 내 풀이 단일 조회
    public SolutionResponse findOne(Long id, Long userId) {
        Solution solution = getOwnedSolution(id, userId);
        return SolutionResponse.from(solution);
    }

    // 풀이 등록(upsert: 이미 있으면 코드 갱신, 없으면 새로 생성)
    @Transactional
    public SolutionResponse create(Long userId, Long problemId, String code, String language) {
        var existing = solutionRepository.findByUserIdAndProblemId(userId, problemId);
        if (existing.isPresent()) {
            Solution solution = existing.get();
            solution.updateCode(code, language);
            return SolutionResponse.from(solution);
        }

        // 신규 등록
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        Solution solution = solutionRepository.save(
                Solution.builder().userId(userId).problem(problem).code(code).language(language).build());

        maybeNotifyTierUp(userId, problem); // 새 풀이 + 해당 티어 패밀리 첫 해결이면 알림
        return SolutionResponse.from(solution);
    }

    // 이 문제의 티어 패밀리(예: GOLD)를 처음 푼 것이면 TIER_UP 알림
    private void maybeNotifyTierUp(Long userId, Problem problem) {
        Tier tier = problem.getTier();
        if (tier == null) return;

        String family = tier.name().substring(0, tier.name().indexOf('_')); // GOLD_II → "GOLD"
        List<Tier> familyTiers = Arrays.stream(Tier.values())
                .filter(t -> t.name().startsWith(family + "_"))
                .toList();

        long prior = solutionRepository.countByUserInTiers(userId, problem.getId(), familyTiers);
        if (prior == 0) {
            notificationService.create(userId, NotificationType.TIER_UP,
                    Map.<String, Object>of("family", family,
                            "problemId", problem.getId(),
                            "problemTitle", problem.getTitle()));
        }
    }

    // 풀이 수정
    @Transactional
    public SolutionResponse update(Long id, Long userId, String code, String language) {
        Solution solution = getOwnedSolution(id, userId);
        solution.updateCode(code, language);
        return SolutionResponse.from(solution);
    }

    // 별 표시 토글
    @Transactional
    public SolutionResponse toggleStar(Long id, Long userId) {
        Solution solution = getOwnedSolution(id, userId);
        solution.toggleStar();
        return SolutionResponse.from(solution);
    }

    // 메모 등록(upsert: 이미 있으면 갱신, 없으면 새로 생성)
    @Transactional
    public MemoResponse upsertMemo(Long solutionId, Long userId,
                                   String wrongReason, String logic, String keyFunctions, String freeNote) {
        Solution solution = getOwnedSolution(solutionId, userId);
        Memo memo = memoRepository.findBySolutionId(solutionId)
                .map(existing -> {
                    existing.update(wrongReason, logic, keyFunctions, freeNote);
                    return existing;
                })
                .orElseGet(() -> memoRepository.save(Memo.builder()
                        .solution(solution)
                        .wrongReason(wrongReason)
                        .logic(logic)
                        .keyFunctions(keyFunctions)
                        .freeNote(freeNote)
                        .build()));
        return MemoResponse.from(memo);
    }

    // 풀이 삭제
    @Transactional
    public void remove(Long id, Long userId) {
        Solution solution = getOwnedSolution(id, userId);
        memoRepository.findBySolutionId(id).ifPresent(memoRepository::delete); // 자식 먼저 지우기
        solutionRepository.delete(solution);
    }

    // 소유권 헬퍼: 아예 없으면 404(not_found), 내 것이 아니면 403(forbidden)
    private Solution getOwnedSolution(Long id, Long userId) {
        // 404
        Solution solution = solutionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLUTION_NOT_FOUND));

        // 403
        if (!solution.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return solution;
    }
}
