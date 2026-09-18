package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import dev.akbayin.fametrics.service.ValuationService;
import org.springframework.stereotype.Component;

/**
 * Runs one screener candidate through the existing, unmodified valuation
 * engine — an in-process bean call (constructor-injected {@link
 * ValuationService}), never a self HTTP call.
 * <p>
 * Takes a required {@link CachedPrice}, not an {@code Optional} one: a
 * candidate with no cached price (no ticker resolved, or FMP had nothing
 * for it) has nothing meaningful to contribute to a price-based ranking, so
 * filtering those out is the caller's job, before reaching this class —
 * same "drop the candidate, don't fail the batch" principle used throughout
 * the screener and the ticker/price lookups.
 */
@Component
public class CandidateAssessor {

    private final ValuationService valuationService;

    public CandidateAssessor(ValuationService valuationService) {
        this.valuationService = valuationService;
    }

    public AssessedCandidate assess(ScreenerCandidate candidate, CachedPrice price) {
        var request = SummaryRequestFactory.from(candidate, price);
        var summary = valuationService.assessSummary(request);
        return new AssessedCandidate(candidate, price, summary);
    }
}
