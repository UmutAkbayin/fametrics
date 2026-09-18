package dev.akbayin.fametrics.ranking;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Combines the screener's fundamentals-only quality_score with core's
 * price-dependent {@link ValueScore#composite()} into one final ranking
 * score, weighted 50/50 — a starting point, not a researched value, same
 * spirit as the arbitrary-but-labeled thresholds in RoeAssessor/
 * DeRatioAssessor. Revisit once there's real output to look at.
 * <p>
 * A candidate needs BOTH scores to get a finalScore — unlike the
 * "average of whichever sub-components are present" policy used inside
 * quality_score and ValueScore.composite() themselves. Those blend several
 * redundant signals of the *same* factor (e.g. ROE and net margin both
 * measure quality), where papering over one missing signal is reasonable.
 * quality_score and Value Score are two genuinely different factors
 * (fundamentals vs. price), and this list exists specifically to surface
 * candidates that are good on both — a candidate missing an entire factor
 * hasn't been shown to satisfy that, so it doesn't get a finalScore rather
 * than being scored on the one factor it does have.
 */
@Component
public class FinalScoreCalculator {

    private static final BigDecimal QUALITY_WEIGHT = new BigDecimal("0.5");
    private static final BigDecimal VALUE_WEIGHT = new BigDecimal("0.5");

    public FinalCandidate score(RankedCandidate ranked) {
        BigDecimal qualityScore = ranked.assessed().candidate().qualityScore();
        BigDecimal valueComposite = ranked.valueScore().composite();

        BigDecimal finalScore = (qualityScore == null || valueComposite == null)
            ? null
            : qualityScore.multiply(QUALITY_WEIGHT)
                .add(valueComposite.multiply(VALUE_WEIGHT))
                .setScale(2, RoundingMode.HALF_UP);

        return new FinalCandidate(ranked, finalScore);
    }

    /**
     * Scores every candidate, drops the ones with no finalScore (missing
     * either quality_score or Value Score), sorts descending, and keeps the
     * top {@code limit}.
     */
    public List<FinalCandidate> rankTop(List<RankedCandidate> candidates, int limit) {
        return candidates.stream()
            .map(this::score)
            .filter(fc -> fc.finalScore() != null)
            .sorted(Comparator.comparing(FinalCandidate::finalScore).reversed())
            .limit(limit)
            .toList();
    }
}
