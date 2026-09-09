package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.SummaryRequest;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Judges an already-computed metric value against a benchmark, per metric type.
 * One implementation per {@link Metric}, so each metric owns its own rules.
 */
public interface MetricAssessor {

    Metric metric();

    String description();

    MetricEvaluation evaluate(SummaryRequest request, BigDecimal value);

    String interpretation(SummaryRequest request, BigDecimal value, Assessment assessment);

    Optional<BigDecimal> calculate(SummaryRequest request);

    default boolean anyNonPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value == null || value.signum() <= 0) {
                return true;
            }
        }
        return false;
    }
}