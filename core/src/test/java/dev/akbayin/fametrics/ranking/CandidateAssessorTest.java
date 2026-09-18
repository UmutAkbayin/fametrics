package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.domain.DeRatioAssessor;
import dev.akbayin.fametrics.domain.GrahamNumberAssessor;
import dev.akbayin.fametrics.domain.Metric;
import dev.akbayin.fametrics.domain.PbRatioAssessor;
import dev.akbayin.fametrics.domain.PeTtmAssessor;
import dev.akbayin.fametrics.domain.RoeAssessor;
import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import dev.akbayin.fametrics.service.ValuationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs a candidate through real assessors (not stubs) — this test's whole
 * point is confirming step 4's design actually works with the existing,
 * unmodified engine end to end, not just that a method got invoked.
 */
class CandidateAssessorTest {

    @Test
    void assess_runsCandidateThroughTheRealUnmodifiedEngine() {
        var valuationService = new ValuationService(List.of(
            new RoeAssessor(), new DeRatioAssessor(), new PeTtmAssessor(),
            new PbRatioAssessor(), new GrahamNumberAssessor()
        ));
        var candidateAssessor = new CandidateAssessor(valuationService);

        var candidate = new ScreenerCandidate(
            1L, "ACME INC", LocalDate.of(2025, 12, 31), "0001-1",
            null, new BigDecimal("500"), new BigDecimal("1000"), new BigDecimal("200"),
            null, null, null, null, null, null, null, null, null, null,
            new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("0.20"), null,
            true, OffsetDateTime.now()
        );
        var price = new CachedPrice(new BigDecimal("45.00"), new BigDecimal("100000"), Instant.now());

        AssessedCandidate assessed = candidateAssessor.assess(candidate, price);

        assertThat(assessed.candidate()).isEqualTo(candidate);
        assertThat(assessed.price()).isEqualTo(price);

        Map<Metric, BigDecimal> valuesByMetric = assessed.summary().metrics().stream()
            .collect(java.util.stream.Collectors.toMap(m -> m.metric(), m -> m.value()));

        // ROE = netIncome(200) / totalEquity(1000) * 100 = 20.00 — the one
        // easy-to-hand-verify value, confirming data actually flowed
        // through correctly rather than just "something non-null" came back.
        assertThat(valuesByMetric.get(Metric.ROE)).isEqualByComparingTo("20.00");
        assertThat(valuesByMetric).containsKeys(Metric.DE_RATIO, Metric.PE_TTM, Metric.PB_RATIO, Metric.GRAHAM_NUMBER);
    }
}
