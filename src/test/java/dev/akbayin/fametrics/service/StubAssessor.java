package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.domain.Assessment;
import dev.akbayin.fametrics.domain.Benchmark;
import dev.akbayin.fametrics.domain.Metric;
import dev.akbayin.fametrics.domain.MetricAssessor;
import dev.akbayin.fametrics.domain.MetricEvaluation;
import dev.akbayin.fametrics.domain.Rating;
import dev.akbayin.fametrics.dto.SummaryRequest;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Test double standing in for a real MetricAssessor, so ValuationService tests
 * exercise only its own orchestration rather than any metric's actual math.
 */
class StubAssessor implements MetricAssessor {

    static final String DESCRIPTION = "stub description";
    static final String INTERPRETATION = "stub interpretation";
    static final MetricEvaluation EVALUATION = new MetricEvaluation(
        new Assessment(Rating.NEUTRAL, "stub label"),
        new Benchmark(BigDecimal.ONE, BigDecimal.TEN, "stub explanation")
    );

    private final Metric metric;
    private final BigDecimal value;

    StubAssessor(Metric metric, BigDecimal value) {
        this.metric = metric;
        this.value = value;
    }

    @Override
    public Metric metric() {
        return metric;
    }

    @Override
    public String description() {
        return DESCRIPTION;
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest request, BigDecimal value) {
        return EVALUATION;
    }

    @Override
    public String interpretation(SummaryRequest request, BigDecimal value, Assessment assessment) {
        return INTERPRETATION;
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest request) {
        return Optional.ofNullable(value);
    }
}
