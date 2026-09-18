package dev.akbayin.fametrics.ranking;

/**
 * One candidate with its engine assessment plus core's own price-dependent
 * Value Score — not yet combined with the screener's quality_score into a
 * final sort order, that's step 6.
 */
public record RankedCandidate(AssessedCandidate assessed, ValueScore valueScore) {
}
