package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.dto.SummaryResponse;
import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.screener.ScreenerCandidate;

/**
 * One candidate after being run through the existing engine: its screener
 * fundamentals, the price snapshot it was priced against, and the full
 * per-metric assessment (value, rating, interpretation) the existing engine
 * produced for it — unmodified engine output, not a new calculation.
 */
public record AssessedCandidate(
    ScreenerCandidate candidate,
    CachedPrice price,
    SummaryResponse summary
) {
}
