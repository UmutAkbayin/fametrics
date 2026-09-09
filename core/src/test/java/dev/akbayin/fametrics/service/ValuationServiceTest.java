package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.domain.Metric;
import dev.akbayin.fametrics.dto.MarketData;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Per-metric calculation and evaluation rules are covered by the individual
 * *AssessorTest classes; these tests only cover ValuationService's own
 * orchestration (metric routing and summary aggregation).
 */
class ValuationServiceTest {

    @Test
    void calculate_whenMetricIsValid_shouldDelegateToMatchingAssessor() {
        var valuationService = new ValuationService(List.of(new StubAssessor(Metric.GRAHAM_NUMBER, new BigDecimal("8.41"))));
        var request = new SummaryRequest(null, null, null);

        var result = valuationService.calculate(request, Metric.GRAHAM_NUMBER);

        assertThat(result).contains(new BigDecimal("8.41"));
    }

    @Test
    void calculate_whenAssessorReturnsEmpty_shouldReturnEmpty() {
        var valuationService = new ValuationService(List.of(new StubAssessor(Metric.GRAHAM_NUMBER, null)));
        var request = new SummaryRequest(null, null, null);

        var result = valuationService.calculate(request, Metric.GRAHAM_NUMBER);

        assertThat(result).isEmpty();
    }

    @Test
    void assess_whenAssessorReturnsValue_shouldReturnMetricResponseWithEvaluationAndInterpretation() {
        var valuationService = new ValuationService(List.of(new StubAssessor(Metric.GRAHAM_NUMBER, new BigDecimal("8.41"))));
        var request = new SummaryRequest(null, null, null);

        var result = valuationService.assess(request, Metric.GRAHAM_NUMBER);

        assertThat(result).isPresent();
        assertThat(result.get().metric()).isEqualTo(Metric.GRAHAM_NUMBER);
        assertThat(result.get().value()).isEqualTo(new BigDecimal("8.41"));
        assertThat(result.get().assessment()).isEqualTo(StubAssessor.EVALUATION.assessment());
        assertThat(result.get().benchmark()).isEqualTo(StubAssessor.EVALUATION.benchmark());
        assertThat(result.get().description()).isEqualTo(StubAssessor.DESCRIPTION);
        assertThat(result.get().interpretation()).isEqualTo(StubAssessor.INTERPRETATION);
    }

    @Test
    void assess_whenAssessorReturnsEmpty_shouldReturnEmpty() {
        var valuationService = new ValuationService(List.of(new StubAssessor(Metric.GRAHAM_NUMBER, null)));
        var request = new SummaryRequest(null, null, null);

        var result = valuationService.assess(request, Metric.GRAHAM_NUMBER);

        assertThat(result).isEmpty();
    }

    @Test
    void assessSummary_shouldOnlyIncludeMetricsWithAssessors() {
        var valuationService = new ValuationService(List.of(new StubAssessor(Metric.GRAHAM_NUMBER, new BigDecimal("8.41"))));
        var request = new SummaryRequest(new MarketData(null, null, null), null, null);

        var result = valuationService.assessSummary(request);

        assertThat(result.metrics()).hasSize(1);
        assertThat(result.metrics().getFirst().metric()).isEqualTo(Metric.GRAHAM_NUMBER);
    }

    @Test
    void assessSummary_shouldOmitMetricsThatCannotBeCalculated() {
        var valuationService = new ValuationService(List.of(
            new StubAssessor(Metric.GRAHAM_NUMBER, new BigDecimal("8.41")),
            new StubAssessor(Metric.PE_TTM, null)
        ));
        var request = new SummaryRequest(null, null, null);

        var result = valuationService.assessSummary(request);

        assertThat(result.metrics()).extracting("metric").containsExactly(Metric.GRAHAM_NUMBER);
    }

    @Test
    void assessSummary_whenNoAssessorsCanCalculate_shouldReturnEmptyList() {
        var valuationService = new ValuationService(List.of(new StubAssessor(Metric.GRAHAM_NUMBER, null)));
        var request = new SummaryRequest(null, null, null);

        var result = valuationService.assessSummary(request);

        assertThat(result.metrics()).isEmpty();
    }
}
