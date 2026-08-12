package com.codeXray.backend.rating.util;

import com.codeXray.backend.problem.entity.Tier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

/**
 * RatingCalculator 단위 테스트.
 *
 * Spring 컨텍스트와 DB 없이 static 메서드만 직접 호출한다.
 * 고정 대상은 세 가지다.
 *   (1) 사전값(prior)의 구성   — ALPHA=2(원 난이도) + BETA=2(정답률 레벨)
 *   (2) shrinkage 감쇠 강도    — 사전값 총 무게 4 대 피드백 1건당 무게 1
 *   (3) 레벨 → 티어 매핑의 경계 — [0, 4.9999] clamp 와 서브티어 3등분
 *
 * 기대값은 모두 코드의 수식에서 손으로 계산했고 계산 과정을 각 테스트 주석에 남겼다.
 * 집계식 결과는 double 나눗셈이라 이진 표현이 정확하지 않을 수 있어 within(1e-12) 허용오차로
 * 비교한다. 다만 acceptanceRateToLevel 의 매핑값과 티어 enum 은 정확 비교한다.
 */
class RatingCalculatorTest {

    // ---------------------------------------------------------------
    // 1) 사전값(prior)의 구성
    // ---------------------------------------------------------------

    @Test
    @DisplayName("정답률이 null이면 사전값에서 정답률 항이 빠져 피드백 0건일 때 원 난이도가 그대로 유지된다")
    void 정답률_null_피드백_0건이면_원_난이도_유지() {
        // (ALPHA·orig) / ALPHA = (2·3) / 2 = 3.0
        // acceptanceRate 가 null 이면 BETA 항이 분자·분모 양쪽에서 빠진다.
        double adjusted = RatingCalculator.computeAdjustedLevel(3, null, List.of());

        assertThat(adjusted).isCloseTo(3.0, within(1e-12));
    }

    @Test
    @DisplayName("정답률이 있으면 사전값은 원 난이도가 아니라 원 난이도와 정답률 레벨의 가중 평균이다")
    void 정답률이_있으면_사전값은_원_난이도와_정답률_레벨의_가중평균() {
        // arLevel = 5·(1 − 75/100) = 5·0.25 = 1.25
        // (ALPHA·orig + BETA·arLevel) / (ALPHA + BETA)
        //   = (2·3 + 2·1.25) / 4 = (6 + 2.5) / 4 = 8.5 / 4 = 2.125
        // 피드백이 0건이어도 원 난이도 3 이 그대로 남지 않는다. ALPHA 와 BETA 가 둘 다
        // 사전값 가중치이므로 사전값 자체가 두 신호의 50:50 혼합이다.
        double adjusted = RatingCalculator.computeAdjustedLevel(3, 75.0, List.of());

        assertThat(adjusted).isCloseTo(2.125, within(1e-12));
        assertThat(adjusted).isNotEqualTo(3.0);
    }

    // ---------------------------------------------------------------
    // 2) shrinkage 감쇠 강도
    // ---------------------------------------------------------------

    @Test
    @DisplayName("극단적인 피드백 1건은 사전값 총 무게 4에 눌려 결과가 피드백 원값이 아니라 사전값 근처에 머무른다")
    void 피드백_1건_극단값은_사전값_쪽으로_수축된다() {
        // arLevel = 5·(1 − 100/100) = 0.0
        // 사전값만 보면 (2·1 + 2·0) / 4 = 2 / 4 = 0.5
        // 피드백 5 를 1건 반영하면 (2·1 + 2·0 + 5) / (2 + 2 + 1) = 7 / 5 = 1.4
        // 피드백 원값 5 까지 끌려가지 않고 사전값 0.5 쪽에 붙어 있는 것이 shrinkage 의 목적이다.
        double prior = RatingCalculator.computeAdjustedLevel(1, 100.0, List.of());
        double adjusted = RatingCalculator.computeAdjustedLevel(1, 100.0, List.of(5));

        assertThat(prior).isCloseTo(0.5, within(1e-12));
        assertThat(adjusted).isCloseTo(1.4, within(1e-12));

        // 사전값까지의 거리(0.9)가 피드백 원값까지의 거리(3.6)보다 훨씬 가깝다.
        assertThat(Math.abs(adjusted - prior)).isLessThan(Math.abs(adjusted - 5.0));
    }

    @Test
    @DisplayName("피드백이 쌓이면 결과는 사전값과 모든 피드백의 가중 평균과 정확히 일치한다")
    void 피드백이_쌓이면_가중평균과_일치() {
        // arLevel = 5·(1 − 50/100) = 5·0.5 = 2.5
        // 분자 = ALPHA·orig + BETA·arLevel + Σfeedback
        //      = 2·2 + 2·2.5 + (4 + 4 + 5 + 5) = 4 + 5 + 18 = 27
        // 분모 = ALPHA + BETA + n = 2 + 2 + 4 = 8
        // 27 / 8 = 3.375
        double adjusted = RatingCalculator.computeAdjustedLevel(2, 50.0, List.of(4, 4, 5, 5));

        assertThat(adjusted).isCloseTo(3.375, within(1e-12));
    }

    // ---------------------------------------------------------------
    // 3) 정답률 → 레벨 척도 매핑 (집계와 분리해서 직접 단정)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("정답률 0%는 레벨 척도 최댓값 5.0으로, 100%는 최솟값 0.0으로 반전 매핑된다")
    void 정답률_경계값이_레벨_척도_양끝으로_매핑된다() {
        // 5·(1 − 0/100)   = 5·1 = 5.0  (아무도 못 푸는 문제 → 최고 난이도)
        // 5·(1 − 100/100) = 5·0 = 0.0  (모두가 푸는 문제   → 최저 난이도)
        // 두 값 모두 이진수로 정확히 표현되므로 허용오차 없이 비교한다.
        assertThat(RatingCalculator.acceptanceRateToLevel(0.0)).isEqualTo(5.0);
        assertThat(RatingCalculator.acceptanceRateToLevel(100.0)).isEqualTo(0.0);

        // null 은 "정답률 정보 없음" 신호로 그대로 통과한다 (집계에서 BETA 항을 건너뛰는 근거).
        assertThat(RatingCalculator.acceptanceRateToLevel(null)).isNull();
    }

    @Test
    @DisplayName("0~100 범위를 벗어난 정답률은 예외를 던지지 않고 경계값으로 clamp된다")
    void 범위_밖_정답률은_예외_없이_clamp된다() {
        // Math.max(0, Math.min(100, rate)) 로 먼저 잘라낸다.
        //  -10 → clamp 0   → 5·(1 − 0/100)   = 5.0
        //  150 → clamp 100 → 5·(1 − 100/100) = 0.0
        // 검증은 이 메서드가 아니라 SubmitFeedbackRequest 의 @Min/@Max 가 담당하고,
        // 계산기는 어떤 입력이 와도 척도 밖으로 나가지 않는 것만 보장한다.
        assertThatCode(() -> RatingCalculator.acceptanceRateToLevel(-10.0)).doesNotThrowAnyException();

        assertThat(RatingCalculator.acceptanceRateToLevel(-10.0)).isEqualTo(5.0);
        assertThat(RatingCalculator.acceptanceRateToLevel(150.0)).isEqualTo(0.0);
    }

    // ---------------------------------------------------------------
    // 4) 정답률 경계가 집계까지 전달되는지
    // ---------------------------------------------------------------

    @Test
    @DisplayName("정답률 0%는 원 난이도가 최저(0)여도 집계 결과를 GOLD_II까지 끌어올린다")
    void 정답률_0퍼센트_경계의_집계_결과() {
        // arLevel = 5.0
        // (2·0 + 2·5.0) / 4 = 10 / 4 = 2.5
        // levelToTier(2.5): familyIdx = 2(GOLD), (2.5 − 2)·3 = 1.5 → floor 1 → SUBS[1] = "II"
        double adjusted = RatingCalculator.computeAdjustedLevel(0, 0.0, List.of());

        assertThat(adjusted).isCloseTo(2.5, within(1e-12));
        assertThat(RatingCalculator.levelToTier(adjusted)).isEqualTo(Tier.GOLD_II);
    }

    @Test
    @DisplayName("정답률 100%는 원 난이도가 최고(5)여도 집계 결과를 GOLD_II까지 끌어내린다")
    void 정답률_100퍼센트_경계의_집계_결과() {
        // arLevel = 0.0
        // (2·5 + 2·0) / 4 = 10 / 4 = 2.5
        // 위 테스트와 정확히 대칭이다. 사전값 무게가 ALPHA:BETA = 1:1 이라
        // 원 난이도와 정답률이 정반대를 가리키면 항상 한가운데로 수렴한다.
        double adjusted = RatingCalculator.computeAdjustedLevel(5, 100.0, List.of());

        assertThat(adjusted).isCloseTo(2.5, within(1e-12));
        assertThat(RatingCalculator.levelToTier(adjusted)).isEqualTo(Tier.GOLD_II);
    }

    // ---------------------------------------------------------------
    // 5) 레벨 → 티어 매핑의 경계
    // ---------------------------------------------------------------

    @Test
    @DisplayName("티어 매핑은 입력이 0 미만이거나 5 이상이어도 BRONZE_III~DIAMOND_I 안에 갇힌다")
    void 티어는_상하한을_벗어나지_않는다() {
        // clamped = Math.max(0, Math.min(4.9999, level))
        //  하한: level ≤ 0 → clamped 0     → familyIdx 0, (0−0)·3 = 0        → BRONZE_III
        //  상한: level ≥ 5 → clamped 4.9999 → familyIdx 4, 0.9999·3 = 2.9997 → floor 2 → DIAMOND_I
        // 상한 clamp 가 없으면 familyIdx 가 5 가 되어 FAMILIES[5] 에서
        // ArrayIndexOutOfBoundsException 이 난다. 이 테스트가 그 방어를 고정한다.
        assertThat(RatingCalculator.levelToTier(-3.0)).isEqualTo(Tier.BRONZE_III);
        assertThat(RatingCalculator.levelToTier(0.0)).isEqualTo(Tier.BRONZE_III);
        assertThat(RatingCalculator.levelToTier(5.0)).isEqualTo(Tier.DIAMOND_I);
        assertThat(RatingCalculator.levelToTier(100.0)).isEqualTo(Tier.DIAMOND_I);

        assertThatCode(() -> RatingCalculator.levelToTier(5.0)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("서브티어 1/3 경계 10곳 중 SILVER 1/3·GOLD 2/3·PLATINUM 2/3·DIAMOND 1/3 네 곳만 부동소수점 오차로 한 칸 아래 서브티어가 나온다")
    void 서브티어_1_3_경계의_부동소수점_동작() {
        // ── 이 테스트는 현재 동작을 고정한 것이며 의도된 동작이 아니다.
        //    경계 처리를 수정하면 이 테스트도 함께 바꿔야 한다. ──
        //
        // subIdx = floor((clamped − familyIdx) · 3) 에서 1/3 은 이진수로 유한 표현이 안 된다.
        // 경계값의 최근접 double 이 참값보다 아래로 떨어지면 한 칸 밀리고, 위로 올라가면 밀리지 않는다.
        // familyIdx 가 0 인 BRONZE 는 뺄셈이 없어 ×3 의 반올림이 오차를 1.0/2.0 으로 복구해 준다.
        //
        //   경계        double 값               (clamped−fam)·3        결과           수학적 기대
        //   BRONZE 1/3  0.3333333333333333      1.0                    BRONZE_II      일치
        //   BRONZE 2/3  0.6666666666666666      2.0                    BRONZE_I       일치
        //   SILVER 1/3  1.3333333333333333      0.9999999999999998     SILVER_III     SILVER_II   ← 밀림
        //   SILVER 2/3  1.6666666666666667      2.0                    SILVER_I       일치
        //   GOLD   1/3  2.3333333333333335      1.0000000000000004     GOLD_II        일치
        //   GOLD   2/3  2.6666666666666665      1.9999999999999996     GOLD_II        GOLD_I      ← 밀림
        //   PLAT   1/3  3.3333333333333335      1.0000000000000004     PLATINUM_II    일치
        //   PLAT   2/3  3.6666666666666665      1.9999999999999996     PLATINUM_II    PLATINUM_I  ← 밀림
        //   DIA    1/3  4.333333333333333       0.9999999999999991     DIAMOND_III    DIAMOND_II  ← 밀림
        //   DIA    2/3  4.666666666666667       2.000000000000001      DIAMOND_I      일치
        //
        // 값은 실제 집계 경로와 같은 단일 나눗셈(num/den) 형태로 만든다.
        // 예를 들어 4.0/3 은 computeAdjustedLevel(2, null, List.of(0)) = (2·2 + 0)/(2 + 1) 로 도달 가능하다.

        // 밀리지 않는 6곳
        assertThat(RatingCalculator.levelToTier(1.0 / 3)).isEqualTo(Tier.BRONZE_II);
        assertThat(RatingCalculator.levelToTier(2.0 / 3)).isEqualTo(Tier.BRONZE_I);
        assertThat(RatingCalculator.levelToTier(5.0 / 3)).isEqualTo(Tier.SILVER_I);
        assertThat(RatingCalculator.levelToTier(7.0 / 3)).isEqualTo(Tier.GOLD_II);
        assertThat(RatingCalculator.levelToTier(10.0 / 3)).isEqualTo(Tier.PLATINUM_II);
        assertThat(RatingCalculator.levelToTier(14.0 / 3)).isEqualTo(Tier.DIAMOND_I);

        // 한 칸 아래로 밀리는 4곳
        assertThat(RatingCalculator.levelToTier(4.0 / 3)).isEqualTo(Tier.SILVER_III);
        assertThat(RatingCalculator.levelToTier(8.0 / 3)).isEqualTo(Tier.GOLD_II);
        assertThat(RatingCalculator.levelToTier(11.0 / 3)).isEqualTo(Tier.PLATINUM_II);
        assertThat(RatingCalculator.levelToTier(13.0 / 3)).isEqualTo(Tier.DIAMOND_III);

        // 집계 경로에서 실제로 4/3 이 만들어지는 것까지 확인한다.
        double reachable = RatingCalculator.computeAdjustedLevel(2, null, List.of(0));
        assertThat(reachable).isEqualTo(4.0 / 3);
        assertThat(RatingCalculator.levelToTier(reachable)).isEqualTo(Tier.SILVER_III);
    }
}
