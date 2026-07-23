package com.codeXray.backend.rating.util;

import com.codeXray.backend.problem.entity.Tier;

import java.util.List;

// DB, Spring 의존성 없음 → 단위 테스트하기 쉬움
public final class RatingCalculator {

    private RatingCalculator() {}

    // 집계식 가중치(prior)
    private static final double ALPHA = 2; // 원본 레벨의 가중치
    private static final double BETA = 2;  // 정답률 기반 레벨의 가중치

    // 티어 family(레벨 구간) × sub(구간 내 3등분). 순서가 곧 의미라 배열로 고정.
    private static final String[] FAMILIES = {"BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND"};
    private static final String[] SUBS = {"III", "II", "I"};

    // 정답률(0~100)을 레벨 척도(0~5)로 변환
    // 정답률이 낮을수록 어려운 문제 → 높은 레벨(100% → 0, 0% → 5, 선형)
    public static Double acceptanceRateToLevel(Double rate) {
        if (rate == null) return null;
        double clamped = Math.max(0, Math.min(100, rate));
        return 5 * (1 - clamped / 100);
    }

    // Bayesian shrinkage 집계
    // (α·origLevel + β·arLevel + Σfeedback) / (α + β + n)
    // 피드백 0개면 원본 + 정답률 평균으로 fallback, 피드백이 쌓일수록 사용자 평균으로 점진 이동
    public static double computeAdjustedLevel(int origLevel, Double acceptanceRate, List<Integer> feedbackLevels) {
        Double arLevel = acceptanceRateToLevel(acceptanceRate);

        double numerator = ALPHA * origLevel;
        double denominator = ALPHA;

        if (arLevel != null) {
            numerator += BETA * arLevel;
            denominator += BETA;
        }

        for (int fb : feedbackLevels) {
            numerator += fb;
            denominator += 1;
        }

        return numerator / denominator;
    }

    // 보정 레벨(0.0~5.0) → 15단계 Tier enum
    // 0~1: BRONZE, 1~2: SILVER, 2~3: GOLD, 3~4: PLATINUM, 4~5: DIAMOND
    // 각 구간을 3등분해서 III → II → I (sub-tier) 결정
    public static Tier levelToTier(double level) {
        double clamped = Math.max(0, Math.min(4.9999, level));
        int familyIdx = (int) Math.floor(clamped);
        int subIdx = (int) Math.floor((clamped - familyIdx) * 3);
        return Tier.valueOf(FAMILIES[familyIdx] + "_" + SUBS[subIdx]);
    }
}
