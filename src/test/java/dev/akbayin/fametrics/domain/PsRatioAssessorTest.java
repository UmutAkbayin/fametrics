package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.MarketData;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PsRatioAssessorTest {

    private final PsRatioAssessor assessor = new PsRatioAssessor();

    @Test
    void evaluate_whenPsRatioIsBelowOne_shouldReturnFavorable() {
        var marketData = new MarketData(new BigDecimal("40.00"), new BigDecimal("50.00"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("0.80"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenPsRatioIsBetweenOneAndThree_shouldReturnNeutral() {
        var marketData = new MarketData(new BigDecimal("100.00"), new BigDecimal("50.00"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("2.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenPsRatioIsAboveThree_shouldReturnUnfavorable() {
        var marketData = new MarketData(new BigDecimal("200.00"), new BigDecimal("50.00"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("4.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_shouldUseUndervaluedAndOvervaluedThresholdsAsBenchmarkBounds() {
        var marketData = new MarketData(new BigDecimal("100.00"), new BigDecimal("50.00"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("2.00"));

        assertThat(evaluation.benchmark().lowerBound()).isEqualByComparingTo("1");
        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("3");
    }
}