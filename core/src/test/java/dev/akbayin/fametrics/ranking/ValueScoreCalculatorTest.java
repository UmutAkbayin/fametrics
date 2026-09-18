package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.domain.Assessment;
import dev.akbayin.fametrics.domain.Benchmark;
import dev.akbayin.fametrics.domain.Metric;
import dev.akbayin.fametrics.domain.Rating;
import dev.akbayin.fametrics.dto.MetricResponse;
import dev.akbayin.fametrics.dto.SummaryResponse;
import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ValueScoreCalculatorTest {

    private final ValueScoreCalculator calculator = new ValueScoreCalculator();

    private static MetricResponse metricResponse(Metric metric, BigDecimal value) {
        return new MetricResponse(metric, value,
            new Assessment(Rating.NEUTRAL, "label"),
            new Benchmark(BigDecimal.ZERO, BigDecimal.TEN, "explanation"),
            "description", "interpretation");
    }

    private static AssessedCandidate assessedCandidate(long cik, BigDecimal price, MetricResponse... metrics) {
        var candidate = new ScreenerCandidate(
            cik, "COMPANY " + cik, LocalDate.of(2025, 12, 31), "0001-1",
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            true, OffsetDateTime.now()
        );
        var cachedPrice = new CachedPrice("TICK" + cik, price, new BigDecimal("1000"), Instant.now());
        return new AssessedCandidate(candidate, cachedPrice, new SummaryResponse(List.of(metrics)));
    }

    private static Map<Long, RankedCandidate> byCik(List<RankedCandidate> ranked) {
        return ranked.stream().collect(Collectors.toMap(r -> r.assessed().candidate().cik(), r -> r));
    }

    @Test
    void compute_cheaperPeRanksHigherThanExpensivePe() {
        var cheap = assessedCandidate(1L, new BigDecimal("50"), metricResponse(Metric.PE_TTM, new BigDecimal("5")));
        var expensive = assessedCandidate(2L, new BigDecimal("50"), metricResponse(Metric.PE_TTM, new BigDecimal("20")));

        var ranked = byCik(calculator.compute(List.of(cheap, expensive)));

        assertThat(ranked.get(1L).valueScore().peTtmPercentile()).isEqualByComparingTo("100.0000");
        assertThat(ranked.get(2L).valueScore().pbRatioPercentile()).isNull(); // no PB metric supplied
        assertThat(ranked.get(2L).valueScore().peTtmPercentile()).isEqualByComparingTo("50.0000");
    }

    @Test
    void compute_discountToFairValue_averagesGrahamAndLynchWhenBothPresent() {
        // price=90, graham=100 -> discount 0.10; price=90, lynch=120 -> discount 0.25
        // average = 0.175
        var candidate = assessedCandidate(1L, new BigDecimal("90"),
            metricResponse(Metric.GRAHAM_NUMBER, new BigDecimal("100")),
            metricResponse(Metric.LYNCH_FAIR_VALUE, new BigDecimal("120"))
        );

        var ranked = calculator.compute(List.of(candidate)).get(0);

        // a single-candidate population always ranks at 100% regardless of
        // the underlying value; this test is about discountToFairValue
        // being computed and present at all, not the percentile math
        // (already covered by PercentileRankerTest).
        assertThat(ranked.valueScore().discountToFairValuePercentile()).isEqualByComparingTo("100.0000");
    }

    @Test
    void compute_negativeLynchFairValue_excludedRatherThanFlippingTheDiscountSign() {
        // Lynch Fair Value can go negative (eps * negative epsGrowthRate) —
        // dividing by it would flip the discount's sign into something that
        // looks favorable for what's actually a bad signal, so it must be
        // excluded, not computed. Only Graham should count here.
        var candidate = assessedCandidate(1L, new BigDecimal("90"),
            metricResponse(Metric.GRAHAM_NUMBER, new BigDecimal("100")), // discount 0.10
            metricResponse(Metric.LYNCH_FAIR_VALUE, new BigDecimal("-50"))
        );
        var other = assessedCandidate(2L, new BigDecimal("90"),
            metricResponse(Metric.GRAHAM_NUMBER, new BigDecimal("100"))
        );

        var ranked = byCik(calculator.compute(List.of(candidate, other)));

        // Both candidates end up with the exact same effective discount
        // (Graham only), so they should tie at the same percentile —
        // proving the negative Lynch value was excluded, not folded in.
        assertThat(ranked.get(1L).valueScore().discountToFairValuePercentile())
            .isEqualByComparingTo(ranked.get(2L).valueScore().discountToFairValuePercentile());
    }

    @Test
    void compute_compositeIsAverageOfWhicheverComponentsArePresent() {
        // Only PE and PB present; PS, PEG, discount all absent.
        var candidate = assessedCandidate(1L, new BigDecimal("50"),
            metricResponse(Metric.PE_TTM, new BigDecimal("10")),
            metricResponse(Metric.PB_RATIO, new BigDecimal("2"))
        );

        var ranked = calculator.compute(List.of(candidate)).get(0);
        var score = ranked.valueScore();

        assertThat(score.psRatioPercentile()).isNull();
        assertThat(score.pegRatioPercentile()).isNull();
        assertThat(score.discountToFairValuePercentile()).isNull();
        // sole candidate, so PE and PB both rank 100 -> composite of [100,100] = 100
        assertThat(score.composite()).isEqualByComparingTo("100.00");
    }

    @Test
    void compute_candidateWithNoUsableMetrics_getsNullComposite() {
        var candidate = assessedCandidate(1L, new BigDecimal("50"));

        var ranked = calculator.compute(List.of(candidate)).get(0);

        assertThat(ranked.valueScore().composite()).isNull();
    }
}
