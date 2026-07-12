package com.codeXray.backend.solution.service;

import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.repository.ProblemRepository;
import com.codeXray.backend.solution.dto.MemoResponse;
import com.codeXray.backend.solution.dto.SolutionResponse;
import com.codeXray.backend.solution.entity.Memo;
import com.codeXray.backend.solution.entity.Solution;
import com.codeXray.backend.solution.repository.MemoRepository;
import com.codeXray.backend.solution.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionService {

    private final SolutionRepository solutionRepository;
    private final MemoRepository memoRepository;
    private final ProblemRepository problemRepository;

    // 내 풀이 전체 조회
    public List<SolutionResponse> findMyAll(Long userId, Boolean starred) {
        List<Solution> list = (starred == null)
                ? solutionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                : solutionRepository.findByUserIdAndStarredOrderByUpdatedAtDesc(userId, starred);
        return list.stream().map(SolutionResponse::from).toList();
    }

    // 내 풀이 단일 조회
    public SolutionResponse findOne(Long id, Long userId) {
        Solution solution = getOwnedSolution(id, userId);
        return SolutionResponse.from(solution);
    }

    // 풀이 등록(upsert: 이미 있으면 코드 갱신, 없으면 새로 생성)
    @Transactional
    public SolutionResponse create(Long userId, Long problemId, String code, String language) {
        Solution solution = solutionRepository.findByUserIdAndProblemId(userId, problemId)
                // 이미 풀이가 있으면 update
                .map(existing -> {
                    existing.updateCode(code, language);
                    return existing;
                // 없으면 생성
                }).orElseGet(() -> {
                    Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
                    return solutionRepository.save(
                            Solution.builder()
                                    .userId(userId)
                                    .problem(problem)
                                    .code(code)
                                    .language(language)
                                    .build()
                    );
                });
        return SolutionResponse.from(solution);
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
        // TODO 5: getOwnedSolution → solution.toggleStar() → from
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
                .orElseGet(() -> {
                    return memoRepository.save(
                            Memo.builder()
                                    .solution(solution)
                                    .wrongReason(wrongReason)
                                    .logic(logic)
                                    .keyFunctions(keyFunctions)
                                    .freeNote(freeNote)
                                    .build()
                    );
                });
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
