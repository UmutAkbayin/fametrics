package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.FundamentalData;
import dev.akbayin.fametrics.dto.MarketData;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LynchFairValueAssessorTest {

    private final LynchFairValueAssessor assessor = new LynchFairValueAssessor();

    @Test
    void evaluate_whenSharePriceIsWellBelowFairValue_shouldReturnFavorable() {
        var marketData = new MarketData(new BigDecimal("35.00"), new BigDecimal("2.93"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.15"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenSharePriceIsCloseToFairValue_shouldReturnNeutral() {
        var marketData = new MarketData(new BigDecimal("44.00"), new BigDecimal("2.93"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.15"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenSharePriceIsWellAboveFairValue_shouldReturnUnfavorable() {
        var marketData = new MarketData(new BigDecimal("60.00"), new BigDecimal("2.93"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.15"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_whenSharePriceIsMissing_shouldReturnNotMeaningful() {
        var marketData = new MarketData(null, new BigDecimal("2.93"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.15"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NOT_MEANINGFUL);
    }

    @Test
    void evaluate_whenMarketDataOrFundamentalDataIsMissing_shouldReturnNotMeaningful() {
        var request = new SummaryRequest(null, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NOT_MEANINGFUL);
    }

    @Test
    void evaluate_shouldUseFairValueAsBenchmarkUpperBound() {
        var marketData = new MarketData(null, new BigDecimal("2.93"), null);
        var fundamentalData = new FundamentalData(null, null, new BigDecimal("0.15"));
        var request = new SummaryRequest(marketData, fundamentalData, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("43.95");
        assertThat(evaluation.benchmark().lowerBound()).isNull();
    }
}
