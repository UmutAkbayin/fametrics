package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryRequestFactoryTest {

    // Every value is distinct so a field-transposition bug (e.g. liabilities
    // and netIncome swapped, both being plain BigDecimal) would actually be
    // caught by asserting exact values, not just "something non-null".
    private static ScreenerCandidate candidateWithDistinctValues() {
        return new ScreenerCandidate(
            1L, "ACME INC", LocalDate.of(2025, 12, 31), "0001-1",
            new BigDecimal("1000"),  // assets
            new BigDecimal("2000"),  // liabilities
            new BigDecimal("3000"),  // stockholdersEquity
            new BigDecimal("4000"),  // netIncome
            new BigDecimal("5000"),  // operatingIncome
            new BigDecimal("6000"),  // operatingCashFlow
            new BigDecimal("7000"),  // capex
            new BigDecimal("8000"),  // revenue
            new BigDecimal("9000"),  // cash
            new BigDecimal("10000"), // longTermDebt
            new BigDecimal("11000"), // sharesOutstanding
            new BigDecimal("12000"), // operatingCashFlowPrior
            new BigDecimal("13000"), // capexPrior
            new BigDecimal("14000"), // netIncomePrior
            new BigDecimal("15.5"),  // eps
            new BigDecimal("16.5"),  // bvps
            new BigDecimal("0.17"),  // epsGrowthRate
            new BigDecimal("80"),    // qualityScore
            true, OffsetDateTime.now()
        );
    }

    @Test
    void from_mapsEachFieldToTheCorrectDestination() {
        var candidate = candidateWithDistinctValues();
        var price = new CachedPrice(new BigDecimal("100.25"), new BigDecimal("999999"), Instant.now());

        var request = SummaryRequestFactory.from(candidate, price);

        assertThat(request.marketData().sharePrice()).isEqualByComparingTo("100.25");
        assertThat(request.marketData().eps()).isEqualByComparingTo("15.5");
        assertThat(request.marketData().bvps()).isEqualByComparingTo("16.5");

        assertThat(request.fundamentalData().marketCap()).isEqualByComparingTo("999999");
        assertThat(request.fundamentalData().totalRevenue()).isEqualByComparingTo("8000");
        assertThat(request.fundamentalData().epsGrowthRate()).isEqualByComparingTo("0.17");

        assertThat(request.capitalStructure().totalLiabilities()).isEqualByComparingTo("2000");
        assertThat(request.capitalStructure().totalEquity()).isEqualByComparingTo("3000");
        assertThat(request.capitalStructure().netIncome()).isEqualByComparingTo("4000");
    }

    @Test
    void from_missingScreenerFundamentals_passThroughAsNullRatherThanThrowing() {
        var candidate = new ScreenerCandidate(
            1L, "NO DATA CO", LocalDate.of(2025, 12, 31), "0001-1",
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            false, OffsetDateTime.now()
        );
        var price = new CachedPrice(new BigDecimal("10.00"), new BigDecimal("100"), Instant.now());

        var request = SummaryRequestFactory.from(candidate, price);

        assertThat(request.marketData().eps()).isNull();
        assertThat(request.marketData().bvps()).isNull();
        assertThat(request.capitalStructure().totalLiabilities()).isNull();
        assertThat(request.capitalStructure().totalEquity()).isNull();
        assertThat(request.capitalStructure().netIncome()).isNull();
        assertThat(request.fundamentalData().totalRevenue()).isNull();
        assertThat(request.fundamentalData().epsGrowthRate()).isNull();
        // price data was present, so those fields should still be populated
        assertThat(request.marketData().sharePrice()).isEqualByComparingTo("10.00");
        assertThat(request.fundamentalData().marketCap()).isEqualByComparingTo("100");
    }
}
