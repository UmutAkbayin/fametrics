package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.CapitalStructure;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RoeAssessorTest {

    private final RoeAssessor assessor = new RoeAssessor();

    @Test
    void calculate_shouldReturnResultOnThePercentageScaleEvaluateExpects() {
        var capitalStructure = new CapitalStructure(null, new BigDecimal("500.00"), new BigDecimal("100.00"));
        var request = new SummaryRequest(null, null, capitalStructure);

        var result = assessor.calculate(request);

        // netIncome 100 / totalEquity 500 = 0.20 -> scaled to 20.00, not 0.20,
        // so it lands correctly against the 15/20 evaluate() thresholds below.
        assertThat(result).isEqualTo(Optional.of(new BigDecimal("20.00")));
    }

    @Test
    void calculate_thenEvaluate_shouldClassifyConsistently() {
        var capitalStructure = new CapitalStructure(null, new BigDecimal("1000.00"), new BigDecimal("100.00"));
        var request = new SummaryRequest(null, null, capitalStructure);

        var roe = assessor.calculate(request).orElseThrow();
        var evaluation = assessor.evaluate(request, roe);

        // netIncome 100 / totalEquity 1000 = 10%, below the 15 threshold.
        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void calculate_whenTotalEquityIsNegative_shouldReturnEmpty() {
        var capitalStructure = new CapitalStructure(null, new BigDecimal("-500.00"), new BigDecimal("-100.00"));
        var request = new SummaryRequest(null, null, capitalStructure);

        var result = assessor.calculate(request);

        assertThat(result).isEmpty();
    }

    @Test
    void evaluate_whenRoeIsBelowFifteen_shouldReturnFavorable() {
        var capitalStructure = new CapitalStructure(null, new BigDecimal("100.00"), new BigDecimal("10.00"));
        var request = new SummaryRequest(null, null, capitalStructure);

        var evaluation = assessor.evaluate(request, new BigDecimal("10.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenRoeIsBetweenFifteenAndTwenty_shouldReturnNeutral() {
        var capitalStructure = new CapitalStructure(null, new BigDecimal("100.00"), new BigDecimal("18.00"));
        var request = new SummaryRequest(null, null, capitalStructure);

        var evaluation = assessor.evaluate(request, new BigDecimal("18.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenRoeIsAboveTwenty_shouldReturnUnfavorable() {
        var capitalStructure = new CapitalStructure(null, new BigDecimal("100.00"), new BigDecimal("25.00"));
        var request = new SummaryRequest(null, null, capitalStructure);

        var evaluation = assessor.evaluate(request, new BigDecimal("25.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_shouldUseUndervaluedAndOvervaluedThresholdsAsBenchmarkBounds() {
        var capitalStructure = new CapitalStructure(null, new BigDecimal("100.00"), new BigDecimal("18.00"));
        var request = new SummaryRequest(null, null, capitalStructure);

        var evaluation = assessor.evaluate(request, new BigDecimal("18.00"));

        assertThat(evaluation.benchmark().lowerBound()).isEqualByComparingTo("15");
        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("20");
    }
}
