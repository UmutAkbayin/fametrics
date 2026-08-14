package dev.akbayin.fametrics.domain;

import java.math.BigDecimal;

public record Benchmark(
    BigDecimal lowerBound,
    BigDecimal upperBound,
    String explanation
) {
}