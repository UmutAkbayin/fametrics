package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.PbRatioRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PbRatioAssessorTest {

    private final PbRatioAssessor assessor = new PbRatioAssessor();

    @Test
    void evaluate_whenPbRatioIsBelowOne_shouldReturnFavorable() {
        var request = new PbRatioRequest(new BigDecimal("40.00"), new BigDecimal("50.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("0.80"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenPbRatioIsBetweenOneAndThree_shouldReturnNeutral() {
        var request = new PbRatioRequest(new BigDecimal("100.00"), new BigDecimal("50.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("2.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenPbRatioIsAboveThree_shouldReturnUnfavorable() {
        var request = new PbRatioRequest(new BigDecimal("200.00"), new BigDecimal("50.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("4.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_shouldUseUndervaluedAndOvervaluedThresholdsAsBenchmarkBounds() {
        var request = new PbRatioRequest(new BigDecimal("100.00"), new BigDecimal("50.00"));

        var evaluation = assessor.evaluate(request, new BigDecimal("2.00"));

        assertThat(evaluation.benchmark().lowerBound()).isEqualByComparingTo("1");
        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("3");
    }
}