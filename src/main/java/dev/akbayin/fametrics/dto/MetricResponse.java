package dev.akbayin.fametrics.dto;

import dev.akbayin.fametrics.domain.Assessment;
import dev.akbayin.fametrics.domain.Benchmark;
import dev.akbayin.fametrics.domain.Metric;

import java.math.BigDecimal;

public record MetricResponse(
    Metric metric,
    BigDecimal value,
    Assessment assessment,
    Benchmark benchmark,
    String description,
    String interpretation
) {
}