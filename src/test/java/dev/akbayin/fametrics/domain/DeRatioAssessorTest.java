package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.CapitalStructure;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DeRatioAssessorTest {

    private final DeRatioAssessor assessor = new DeRatioAssessor();

    @Test
    void evaluate_whenDeRatioIsBelowOne_shouldReturnFavorable() {
        var capitalStructure = new CapitalStructure(new BigDecimal("40.00"), new BigDecimal("50.00"), null);
        var request = new SummaryRequest(null, null, capitalStructure);

        var evaluation = assessor.evaluate(request, new BigDecimal("0.80"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.FAVORABLE);
    }

    @Test
    void evaluate_whenDeRatioIsBetweenOneAndTwo_shouldReturnNeutral() {
        var capitalStructure = new CapitalStructure(new BigDecimal("100.00"), new BigDecimal("50.00"), null);
        var request = new SummaryRequest(null, null, capitalStructure);

        var evaluation = assessor.evaluate(request, new BigDecimal("2.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.NEUTRAL);
    }

    @Test
    void evaluate_whenDeRatioIsAboveTwo_shouldReturnUnfavorable() {
        var capitalStructure = new CapitalStructure(new BigDecimal("200.00"), new BigDecimal("50.00"), null);
        var request = new SummaryRequest(null, null, capitalStructure);

        var evaluation = assessor.evaluate(request, new BigDecimal("4.00"));

        assertThat(evaluation.assessment().rating()).isEqualTo(Rating.UNFAVORABLE);
    }

    @Test
    void evaluate_shouldUseUndervaluedAndOvervaluedThresholdsAsBenchmarkBounds() {
        var capitalStructure = new CapitalStructure(new BigDecimal("100.00"), new BigDecimal("50.00"), null);
        var request = new SummaryRequest(null, null, capitalStructure);

        var evaluation = assessor.evaluate(request, new BigDecimal("2.00"));

        assertThat(evaluation.benchmark().lowerBound()).isEqualByComparingTo("1.00");
        assertThat(evaluation.benchmark().upperBound()).isEqualByComparingTo("2.00");
    }
}
