package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.LynchFairValueRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LynchFairValueAssessorTest {

    private final LynchFairValueAssessor assessor = new LynchFairValueAssessor();

    @Test
    void evaluate_whenSharePriceIsWellBelowFairValue_shouldReturnFavorable() {
        var request = new LynchFairValueRequest(new BigDecimal("2.93"), new BigDecimal("0.15"), new BigDecimal("35.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenSharePriceIsCloseToFairValue_shouldReturnNeutral() {
        var request = new LynchFairValueRequest(new BigDecimal("2.93"), new BigDecimal("0.15"), new BigDecimal("44.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenSharePriceIsWellAboveFairValue_shouldReturnUnfavorable() {
        var request = new LynchFairValueRequest(new BigDecimal("2.93"), new BigDecimal("0.15"), new BigDecimal("60.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_whenSharePriceIsMissing_shouldReturnNotMeaningful() {
        var request = new LynchFairValueRequest(new BigDecimal("2.93"), new BigDecimal("0.15"), null);

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NOT_MEANINGFUL);
    }

    @Test
    void evaluate_shouldUseFairValueAsBenchmarkUpperBound() {
        var request = new LynchFairValueRequest(new BigDecimal("2.93"), new BigDecimal("0.15"), null);

        var evaluation = assessor.evaluate(request, new BigDecimal("43.95"));

        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("43.95");
        assertThat(evaluation.benchmark().lowerBound()).isNull();
    }
}