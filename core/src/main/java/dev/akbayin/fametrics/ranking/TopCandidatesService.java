package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.pricing.PriceCache;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import dev.akbayin.fametrics.screener.ScreenerClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Ties the whole orchestration pipeline together: fetch candidates from the
 * screener, price them from the cache (skipping anything with no cached
 * price — a candidate with no price contributes nothing to a price-based
 * ranking, same principle used throughout this pipeline), run them through
 * the existing engine, compute the Value Score, and return the final
 * top-N sorted list.
 */
@Service
public class TopCandidatesService {

    private static final int CANDIDATE_POOL_SIZE = 200;

    private final ScreenerClient screenerClient;
    private final PriceCache priceCache;
    private final CandidateAssessor candidateAssessor;
    private final ValueScoreCalculator valueScoreCalculator;
    private final FinalScoreCalculator finalScoreCalculator;

    public TopCandidatesService(
        ScreenerClient screenerClient,
        PriceCache priceCache,
        CandidateAssessor candidateAssessor,
        ValueScoreCalculator valueScoreCalculator,
        FinalScoreCalculator finalScoreCalculator
    ) {
        this.screenerClient = screenerClient;
        this.priceCache = priceCache;
        this.candidateAssessor = candidateAssessor;
        this.valueScoreCalculator = valueScoreCalculator;
        this.finalScoreCalculator = finalScoreCalculator;
    }

    public List<FinalCandidate> getTopCandidates(int limit) {
        List<ScreenerCandidate> candidates = screenerClient.fetchCandidates(CANDIDATE_POOL_SIZE);

        List<AssessedCandidate> assessed = new ArrayList<>();
        for (ScreenerCandidate candidate : candidates) {
            priceCache.findPrice(candidate.cik())
                .ifPresent(price -> assessed.add(candidateAssessor.assess(candidate, price)));
        }

        List<RankedCandidate> ranked = valueScoreCalculator.compute(assessed);
        return finalScoreCalculator.rankTop(ranked, limit);
    }
}
