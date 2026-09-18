package dev.akbayin.fametrics.ranking;

import java.math.BigDecimal;

/**
 * core's own price-dependent ranking, separate from the screener's
 * fundamentals-only quality_score. Each field is a 0-100 percentile rank of
 * that metric across the current candidate pool, not the raw metric value —
 * composite is the average of whichever of them are present for this
 * candidate.
 * <p>
 * ROE is deliberately not a component here even though the existing engine
 * computes it: the screener's quality_score already ranks on ROE, and
 * including it again here would silently double-weight it once quality_score
 * and this composite are combined in step 6.
 */
public record ValueScore(
    BigDecimal peTtmPercentile,
    BigDecimal pbRatioPercentile,
    BigDecimal psRatioPercentile,
    BigDecimal pegRatioPercentile,
    BigDecimal discountToFairValuePercentile,
    BigDecimal composite
) {
}
