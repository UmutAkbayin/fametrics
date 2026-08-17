package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.MarketData;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PeTtmAssessorTest {

    private final PeTtmAssessor assessor = new PeTtmAssessor();

    @Test
    void evaluate_whenPeTtmIsBelowFairRange_shouldReturnFavorable() {
        var marketData = new MarketData(new BigDecimal("40.38"), new BigDecimal("2.93"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("13.78"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenPeTtmIsWithinFairRange_shouldReturnNeutral() {
        var marketData = new MarketData(new BigDecimal("400.00"), new BigDecimal("20.00"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("20.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenPeTtmIsAboveFairRange_shouldReturnUnfavorable() {
        var marketData = new MarketData(new BigDecimal("600.00"), new BigDecimal("20.00"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("30.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_whenPeTtmIsExactlyAtLowerBound_shouldReturnNeutral() {
        var marketData = new MarketData(new BigDecimal("300.00"), new BigDecimal("20.00"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("15.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenPeTtmIsExactlyAtUpperBound_shouldReturnNeutral() {
        var marketData = new MarketData(new BigDecimal("500.00"), new BigDecimal("20.00"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("25.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_shouldUseFixedFairRangeAsBenchmark() {
        var marketData = new MarketData(new BigDecimal("40.38"), new BigDecimal("2.93"), null);
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("13.78"));

        assertThat(evaluation.benchmark().lowerBound()).isEqualByComparingTo("15");
        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("25");
    }
}