package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.GrahamRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GrahamNumberAssessorTest {

    private final GrahamNumberAssessor assessor = new GrahamNumberAssessor();

    @Test
    void evaluate_whenSharePriceIsWellBelowGrahamNumber_shouldReturnFavorable() {
        var request = new GrahamRequest(new BigDecimal("2.93"), new BigDecimal("47.65"), new BigDecimal("40.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenSharePriceIsCloseToGrahamNumber_shouldReturnNeutral() {
        var request = new GrahamRequest(new BigDecimal("2.93"), new BigDecimal("47.65"), new BigDecimal("57.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenSharePriceIsWellAboveGrahamNumber_shouldReturnUnfavorable() {
        var request = new GrahamRequest(new BigDecimal("2.93"), new BigDecimal("47.65"), new BigDecimal("80.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_whenSharePriceIsMissing_shouldReturnNotMeaningful() {
        var request = new GrahamRequest(new BigDecimal("2.93"), new BigDecimal("47.65"));

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NOT_MEANINGFUL);
    }

    @Test
    void evaluate_shouldUseGrahamNumberAsBenchmarkUpperBound() {
        var request = new GrahamRequest(new BigDecimal("2.93"), new BigDecimal("47.65"));

        var evaluation = assessor.evaluate(request, new BigDecimal("56.05"));

        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("56.05");
        assertThat(evaluation.benchmark().lowerBound()).isNull();
    }
}