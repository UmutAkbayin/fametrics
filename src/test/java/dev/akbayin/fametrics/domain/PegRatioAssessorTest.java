package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.FundamentalData;
import dev.akbayin.fametrics.dto.MarketData;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PegRatioAssessorTest {

    private final PegRatioAssessor assessor = new PegRatioAssessor();

    @Test
    void evaluate_whenPegRatioIsBelowOne_shouldReturnFavorable() {
        var marketData = new MarketData(new BigDecimal("40.00"), new BigDecimal("2.00"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.15"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("0.80"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenPegRatioIsBetweenOneAndOneAndHalf_shouldReturnNeutral() {
        var marketData = new MarketData(new BigDecimal("40.00"), new BigDecimal("2.00"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.10"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("1.20"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenPegRatioIsAboveOneAndHalf_shouldReturnUnfavorable() {
        var marketData = new MarketData(new BigDecimal("40.00"), new BigDecimal("2.00"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.05"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("2.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_shouldUseUndervaluedAndOvervaluedThresholdsAsBenchmarkBounds() {
        var marketData = new MarketData(new BigDecimal("40.00"), new BigDecimal("2.00"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.10"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("1.20"));

        assertThat(evaluation.benchmark().lowerBound()).isEqualByComparingTo("1");
        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("1.5");
    }
}