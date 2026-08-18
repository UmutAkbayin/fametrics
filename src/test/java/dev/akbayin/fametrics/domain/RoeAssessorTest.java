package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.CapitalStructure;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RoeAssessorTest {

    private final RoeAssessor assessor = new RoeAssessor();

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
