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
        // 칸 번호 idx = floor(level·3) 을 [0, 14] 로 자른다.
        //  하한: level ≤ 0 → idx 0  → BRONZE_III
        //  상한: level ≥ 5 → idx 15 이상 → 14 → DIAMOND_I
        // 상한을 자르지 않으면 FAMILIES[5] 에서 ArrayIndexOutOfBoundsException 이 난다.
        // 시드에 레벨 5·정답률 0% 문제가 있어 정확히 5.0 이 실제로 나온다. 이 테스트가 그 방어를 고정한다.
        assertThat(RatingCalculator.levelToTier(-3.0)).isEqualTo(Tier.BRONZE_III);
        assertThat(RatingCalculator.levelToTier(0.0)).isEqualTo(Tier.BRONZE_III);
        assertThat(RatingCalculator.levelToTier(5.0)).isEqualTo(Tier.DIAMOND_I);
        assertThat(RatingCalculator.levelToTier(100.0)).isEqualTo(Tier.DIAMOND_I);

        assertThatCode(() -> RatingCalculator.levelToTier(5.0)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("1/3 경계 10곳이 모두 수학적 기대 서브티어로 분류된다(부동소수점 오차로 한 칸 밀리지 않는다)")
    void 서브티어_1_3_경계는_모두_수학적_기대와_같다() {
        // 고치기 전에는 SILVER 1/3·GOLD 2/3·PLATINUM 2/3·DIAMOND 1/3 네 곳이 한 칸 아래로 밀렸다.
        // (1.3333333333333333 − 1)·3 = 0.9999999999999998 → floor 0 처럼, 내림을 두 번 하며 오차가 쌓였기 때문.
        // 지금은 level·3 을 소수 6자리에서 반올림한 뒤 한 번만 내린다: 4/3·3 = 4.0 → 칸 4 = SILVER_II
        assertThat(RatingCalculator.levelToTier(1.0 / 3)).isEqualTo(Tier.BRONZE_II);
        assertThat(RatingCalculator.levelToTier(2.0 / 3)).isEqualTo(Tier.BRONZE_I);
        assertThat(RatingCalculator.levelToTier(4.0 / 3)).isEqualTo(Tier.SILVER_II);
        assertThat(RatingCalculator.levelToTier(5.0 / 3)).isEqualTo(Tier.SILVER_I);
        assertThat(RatingCalculator.levelToTier(7.0 / 3)).isEqualTo(Tier.GOLD_II);
        assertThat(RatingCalculator.levelToTier(8.0 / 3)).isEqualTo(Tier.GOLD_I);
        assertThat(RatingCalculator.levelToTier(10.0 / 3)).isEqualTo(Tier.PLATINUM_II);
        assertThat(RatingCalculator.levelToTier(11.0 / 3)).isEqualTo(Tier.PLATINUM_I);
        assertThat(RatingCalculator.levelToTier(13.0 / 3)).isEqualTo(Tier.DIAMOND_II);
        assertThat(RatingCalculator.levelToTier(14.0 / 3)).isEqualTo(Tier.DIAMOND_I);

        // 집계 경로로 4/3 을 만들어도 같다: (2·2 + 0)/(2 + 1)
        double reachable = RatingCalculator.computeAdjustedLevel(2, null, List.of(0));
        assertThat(reachable).isCloseTo(4.0 / 3, within(1e-9));
        assertThat(RatingCalculator.levelToTier(reachable)).isEqualTo(Tier.SILVER_II);
    }

    @Test
    @DisplayName("시드 id 617(레벨 1, 정답률 80%)은 0.9999999999999999 가 아니라 1.0 이 되어 SILVER_III 로 분류된다")
    void 패밀리_경계의_부동소수점_오차를_정리한다() {
        // arLevel = 5·(1 − 80/100) = 5·0.19999999999999996 = 0.9999999999999998 (이진 표현 오차)
        // (2·1 + 2·0.9999999999999998) / 4 = 0.9999999999999999 → 예전에는 BRONZE_I 로 패밀리째 어긋났다
        double adjusted = RatingCalculator.computeAdjustedLevel(1, 80.0, List.of());

        assertThat(adjusted).isEqualTo(1.0);
        assertThat(RatingCalculator.levelToTier(adjusted)).isEqualTo(Tier.SILVER_III);
    }
}
