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

import static org.assertj.core.api.Assertions.assertThat;

class TopCandidateMapperTest {

    @Test
    void toResponse_mapsEveryFieldFromTheRightSource() {
        var candidate = new ScreenerCandidate(
            42L, "ACME INC", LocalDate.of(2025, 12, 31), "0001-1",
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, new BigDecimal("77"),
            true, OffsetDateTime.now()
        );
        var price = new CachedPrice("ACME", new BigDecimal("123.45"), new BigDecimal("999"), Instant.now());
        var metric = new MetricResponse(Metric.ROE, new BigDecimal("20"),
            new Assessment(Rating.FAVORABLE, "label"), new Benchmark(BigDecimal.ZERO, BigDecimal.TEN, "x"),
            "description", "interpretation");
        var assessed = new AssessedCandidate(candidate, price, new SummaryResponse(List.of(metric)));
        var valueScore = new ValueScore(
            new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"),
            new BigDecimal("4"), new BigDecimal("5"), new BigDecimal("6")
        );
        var finalCandidate = new FinalCandidate(new RankedCandidate(assessed, valueScore), new BigDecimal("88.50"));

        var response = TopCandidateMapper.toResponse(finalCandidate);

        assertThat(response.cik()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("ACME INC");
        assertThat(response.ticker()).isEqualTo("ACME");
        assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(response.price()).isEqualByComparingTo("123.45");
        assertThat(response.qualityScore()).isEqualByComparingTo("77");
        assertThat(response.valueScore()).isEqualTo(valueScore);
        assertThat(response.finalScore()).isEqualByComparingTo("88.50");
        assertThat(response.metrics()).containsExactly(metric);
    }
}
