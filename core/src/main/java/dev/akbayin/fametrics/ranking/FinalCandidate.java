package dev.akbayin.fametrics.ranking;

import java.math.BigDecimal;

/**
 * A candidate with its final composite score — see
 * {@link FinalScoreCalculator} for how it's derived. Nothing here is
 * flattened out of {@link RankedCandidate}; step 7's response mapping reads
 * from {@code ranked.assessed().candidate()}, {@code
 * ranked.assessed().summary()}, and {@code ranked.valueScore()} directly
 * rather than this type duplicating those fields.
 */
public record FinalCandidate(RankedCandidate ranked, BigDecimal finalScore) {
}
