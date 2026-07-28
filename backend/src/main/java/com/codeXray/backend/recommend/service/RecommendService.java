package com.codeXray.backend.recommend.service;

import com.codeXray.backend.problem.dto.ProblemResponse;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.entity.Tier;
import com.codeXray.backend.problem.repository.ProblemRepository;
import com.codeXray.backend.rating.repository.LevelFeedbackRepository;
import com.codeXray.backend.recommend.dto.RecommendationResponse;
import com.codeXray.backend.solution.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final int LIMIT = 8;      // 추천 개수
    private static final int WEAK_TAGS = 5;  // 고려할 약점 태그 수
    private static final int EASY_MAX_ORDINAL = 3; // 신규 유저 기본 구간 상한(SILVER_III)

    private final SolutionRepository solutionRepository;
    private final LevelFeedbackRepository levelFeedbackRepository;
    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommend(Long userId) {
        List<Long> solved = solutionRepository.findSolvedProblemIds(userId);
        // 이미 푼 문제 제외. 빈 리스트면 JPQL `not in ()` 오류라 sentinel(-1) 사용.
        List<Long> exclude = solved.isEmpty() ? List.of(-1L) : solved;

        List<String> weakTags = levelFeedbackRepository.weakTags(userId, PageRequest.of(0, WEAK_TAGS))
                .stream().map(w -> w.getTag()).toList();

        List<Tier> band = computeTierBand(solutionRepository.findSolvedTiers(userId));

        // 문제 id 기준 중복 제거 + 삽입 순서 유지 (약점 우선 → 적정 난이도로 채움)
        Map<Long, RecommendationResponse> picks = new LinkedHashMap<>();

        // 1) 약점 태그 보강
        if (!weakTags.isEmpty()) {
            for (Problem p : problemRepository.recommendByTags(weakTags, exclude, PageRequest.of(0, LIMIT))) {
                picks.putIfAbsent(p.getId(),
                        new RecommendationResponse(ProblemResponse.from(p), "약점 보강", firstMatchingTag(p, weakTags)));
            }
        }

        // 2) 적정 난이도로 부족분 채우기
        if (picks.size() < LIMIT && !band.isEmpty()) {
            for (Problem p : problemRepository.recommendByTiers(band, exclude, PageRequest.of(0, LIMIT))) {
                if (picks.size() >= LIMIT) break;
                picks.putIfAbsent(p.getId(),
                        new RecommendationResponse(ProblemResponse.from(p), "적정 난이도", null));
            }
        }

        return picks.values().stream().limit(LIMIT).toList();
    }

    // 푼 문제 티어들의 중앙값 ±1 구간. 없으면(신규) 쉬운 구간.
    private List<Tier> computeTierBand(List<Tier> solvedTiers) {
        Tier[] all = Tier.values();
        if (solvedTiers.isEmpty()) {
            return Arrays.stream(all).filter(t -> t.ordinal() <= EASY_MAX_ORDINAL).toList();
        }
        List<Integer> ords = solvedTiers.stream().map(Tier::ordinal).sorted().toList();
        int median = ords.get(ords.size() / 2);
        int lo = Math.max(0, median - 1);
        int hi = Math.min(all.length - 1, median + 1);
        return Arrays.stream(all).filter(t -> t.ordinal() >= lo && t.ordinal() <= hi).toList();
    }

    // 이 문제가 가진 태그 중 약점 태그와 겹치는 첫 태그(이유 표시용)
    private String firstMatchingTag(Problem p, List<String> weakTags) {
        return p.getProblemTags().stream()
                .map(pt -> pt.getTag().getName())
                .filter(weakTags::contains)
                .findFirst().orElse(null);
    }
}
