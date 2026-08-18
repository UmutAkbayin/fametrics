package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.MarketData;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GrahamNumberAssessorTest {

    private final GrahamNumberAssessor assessor = new GrahamNumberAssessor();

    @Test
    void evaluate_whenSharePriceIsWellBelowGrahamNumber_shouldReturnFavorable() {
        var marketData = new MarketData(new BigDecimal("40.00"), new BigDecimal("2.93"), new BigDecimal("47.65"));
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenSharePriceIsCloseToGrahamNumber_shouldReturnNeutral() {
        var marketData = new MarketData(new BigDecimal("57.00"), new BigDecimal("2.93"), new BigDecimal("47.65"));
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenSharePriceIsWellAboveGrahamNumber_shouldReturnUnfavorable() {
        var marketData = new MarketData(new BigDecimal("80.00"), new BigDecimal("2.93"), new BigDecimal("47.65"));
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_whenSharePriceIsMissing_shouldReturnNotMeaningful() {
        var marketData = new MarketData(null, new BigDecimal("2.93"), new BigDecimal("47.65"));
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NOT_MEANINGFUL);
    }

    @Test
    void evaluate_whenMarketDataIsMissing_shouldReturnNotMeaningful() {
        var request = new SummaryRequest(null, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NOT_MEANINGFUL);
    }

    @Test
    void evaluate_shouldUseGrahamNumberAsBenchmarkUpperBound() {
        var marketData = new MarketData(null, new BigDecimal("2.93"), new BigDecimal("47.65"));
        var request = new SummaryRequest(marketData, null, null);

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("56.05");
        assertThat(evaluation.benchmark().lowerBound()).isNull();
    }
}
