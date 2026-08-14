package dev.akbayin.fametrics.domain;

import java.math.BigDecimal;

/**
 * Judges an already-computed metric value against a benchmark, per metric type.
 * One implementation per {@link Metric}, so each metric owns its own rules.
 */
public interface MetricAssessor<R> {

    Metric metric();

    String description();

    MetricEvaluation evaluate(R request, BigDecimal value);
}