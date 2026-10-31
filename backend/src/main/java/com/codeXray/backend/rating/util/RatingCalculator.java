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

        return snap(numerator / denominator);
    }

    /*
     * 부동소수점 오차 정리. (2×1 + 2×1.0)/4 처럼 수학적으로 정확히 1.0 이어야 할 값이 0.9999999999999999 로 나와
     * 티어 경계(1.0 = SILVER 시작) 아래로 떨어지는 일이 실제 시드(id 617)에서 있었다. 그 문제는 SILVER_III 대신 BRONZE_I 로 분류됐다.
     * 소수 9자리에서 반올림하면 이런 오차는 사라지고, 레벨 척도(0~5)에서 의미 있는 차이는 그대로 남는다
     */
    private static double snap(double value) {
        return Math.round(value * 1e9) / 1e9;
    }

    // 보정 레벨(0.0~5.0) → 15단계 Tier enum
    // 0~1: BRONZE, 1~2: SILVER, 2~3: GOLD, 3~4: PLATINUM, 4~5: DIAMOND
    // 각 구간을 3등분해서 III → II → I (sub-tier) 결정
    // 레벨에 3을 곱하면 15칸(0~14)의 칸 번호가 된다. 칸 번호를 한 번만 내림해서 family 와 sub 를 나눈다.
    // 예전 방식(family 를 내림한 뒤 나머지에 3을 곱해 또 내림)은 4/3 같은 1/3 경계에서 오차가 두 번 쌓여 한 칸 아래로 떨어졌다
    public static Tier levelToTier(double level) {
        double cells = Math.round(level * 3 * 1e6) / 1e6; // 경계 근처 오차(…9999)를 정리
        int idx = (int) Math.floor(cells);
        idx = Math.max(0, Math.min(FAMILIES.length * SUBS.length - 1, idx)); // 0 미만·5 이상은 양 끝 칸
        return Tier.valueOf(FAMILIES[idx / SUBS.length] + "_" + SUBS[idx % SUBS.length]);
    }
}
