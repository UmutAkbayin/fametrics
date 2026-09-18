package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.dto.SummaryResponse;
import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinalScoreCalculatorTest {

    private final FinalScoreCalculator calculator = new FinalScoreCalculator();

    private static ScreenerCandidate candidate(long cik, BigDecimal qualityScore) {
        return new ScreenerCandidate(
            cik, "COMPANY " + cik, LocalDate.of(2025, 12, 31), "0001-1",
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, qualityScore,
            true, OffsetDateTime.now()
        );
    }

    private static RankedCandidate rankedCandidate(long cik, BigDecimal qualityScore, BigDecimal valueComposite) {
        var assessed = new AssessedCandidate(
            candidate(cik, qualityScore),
            new CachedPrice(new BigDecimal("50"), new BigDecimal("1000"), Instant.now()),
            new SummaryResponse(List.of())
        );
        var valueScore = new ValueScore(null, null, null, null, null, valueComposite);
        return new RankedCandidate(assessed, valueScore);
    }

    @Test
    void score_bothPresent_combinesFiftyFifty() {
        var ranked = rankedCandidate(1L, new BigDecimal("80"), new BigDecimal("60"));

        var result = calculator.score(ranked);

        // 80*0.5 + 60*0.5 = 70.00
        assertThat(result.finalScore()).isEqualByComparingTo("70.00");
    }

    @Test
    void score_qualityScoreMissing_returnsNullFinalScore() {
        var ranked = rankedCandidate(1L, null, new BigDecimal("60"));

        var result = calculator.score(ranked);

        assertThat(result.finalScore()).isNull();
    }

    @Test
    void score_valueScoreCompositeMissing_returnsNullFinalScore() {
        var ranked = rankedCandidate(1L, new BigDecimal("80"), null);

        var result = calculator.score(ranked);

        assertThat(result.finalScore()).isNull();
    }

    @Test
    void rankTop_sortsDescendingDropsNullsAndRespectsLimit() {
        var best = rankedCandidate(1L, new BigDecimal("90"), new BigDecimal("90"));   // 90.00
        var middle = rankedCandidate(2L, new BigDecimal("50"), new BigDecimal("50")); // 50.00
        var worst = rankedCandidate(3L, new BigDecimal("10"), new BigDecimal("10"));  // 10.00
        var unscoreable = rankedCandidate(4L, null, new BigDecimal("99"));            // null, must be dropped

        var top = calculator.rankTop(List.of(middle, unscoreable, worst, best), 2);

        assertThat(top).hasSize(2);
        assertThat(top.get(0).ranked()).isEqualTo(best);
        assertThat(top.get(0).finalScore()).isEqualByComparingTo("90.00");
        assertThat(top.get(1).ranked()).isEqualTo(middle);
        assertThat(top).noneMatch(fc -> fc.ranked().equals(unscoreable));
    }

    @Test
    void rankTop_isSortedDescendingByFinalScore() {
        var low = rankedCandidate(1L, new BigDecimal("20"), new BigDecimal("20"));
        var high = rankedCandidate(2L, new BigDecimal("80"), new BigDecimal("80"));

        var top = calculator.rankTop(List.of(low, high), 10);

        assertThat(top).isSortedAccordingTo(Comparator.comparing(FinalCandidate::finalScore, Comparator.reverseOrder()));
    }
}
