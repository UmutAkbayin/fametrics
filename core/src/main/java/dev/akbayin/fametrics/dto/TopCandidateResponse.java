package dev.akbayin.fametrics.dto;

import dev.akbayin.fametrics.ranking.ValueScore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One entry of the frontend-facing Top N list: identity, the price it was
 * ranked against, the screener's fundamentals-only quality_score, core's
 * price-dependent Value Score (and its components), the combined
 * finalScore used to sort, and the full per-metric detail (value, rating,
 * interpretation) from the existing engine — reused directly via
 * {@link MetricResponse} rather than duplicated into a new shape.
 */
public record TopCandidateResponse(
    Long cik,
    String name,
    String ticker,
    LocalDate periodEnd,
    BigDecimal price,
    BigDecimal qualityScore,
    ValueScore valueScore,
    BigDecimal finalScore,
    List<MetricResponse> metrics
) {
}
