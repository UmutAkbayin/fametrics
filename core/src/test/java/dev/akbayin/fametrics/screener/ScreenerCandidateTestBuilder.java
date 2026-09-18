package dev.akbayin.fametrics.screener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * ScreenerCandidate's 24-parameter positional constructor is easy to get
 * subtly wrong in tests — a miscounted null run compiles-fails loudly, but
 * a wrong VALUE in the wrong position wouldn't. This builder is the fix:
 * written and verified once, so no test has to hand-count positional args
 * again. Every field defaults to null (or a harmless placeholder for the
 * non-nullable ones); set only what a given test actually cares about.
 */
public final class ScreenerCandidateTestBuilder {

    private long cik = 1L;
    private String name = "TEST CO";
    private LocalDate periodEnd = LocalDate.of(2025, 12, 31);
    private String adsh = "0001-1";
    private BigDecimal assets;
    private BigDecimal liabilities;
    private BigDecimal stockholdersEquity;
    private BigDecimal netIncome;
    private BigDecimal operatingIncome;
    private BigDecimal operatingCashFlow;
    private BigDecimal capex;
    private BigDecimal revenue;
    private BigDecimal cash;
    private BigDecimal longTermDebt;
    private BigDecimal sharesOutstanding;
    private BigDecimal operatingCashFlowPrior;
    private BigDecimal capexPrior;
    private BigDecimal netIncomePrior;
    private BigDecimal eps;
    private BigDecimal bvps;
    private BigDecimal epsGrowthRate;
    private BigDecimal qualityScore;
    private boolean passesHardFilter = true;
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public static ScreenerCandidateTestBuilder screenerCandidate() {
        return new ScreenerCandidateTestBuilder();
    }

    public ScreenerCandidateTestBuilder cik(long cik) {
        this.cik = cik;
        return this;
    }

    public ScreenerCandidateTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ScreenerCandidateTestBuilder periodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
        return this;
    }

    public ScreenerCandidateTestBuilder adsh(String adsh) {
        this.adsh = adsh;
        return this;
    }

    public ScreenerCandidateTestBuilder assets(BigDecimal assets) {
        this.assets = assets;
        return this;
    }

    public ScreenerCandidateTestBuilder liabilities(BigDecimal liabilities) {
        this.liabilities = liabilities;
        return this;
    }

    public ScreenerCandidateTestBuilder stockholdersEquity(BigDecimal stockholdersEquity) {
        this.stockholdersEquity = stockholdersEquity;
        return this;
    }

    public ScreenerCandidateTestBuilder netIncome(BigDecimal netIncome) {
        this.netIncome = netIncome;
        return this;
    }

    public ScreenerCandidateTestBuilder operatingIncome(BigDecimal operatingIncome) {
        this.operatingIncome = operatingIncome;
        return this;
    }

    public ScreenerCandidateTestBuilder operatingCashFlow(BigDecimal operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow;
        return this;
    }

    public ScreenerCandidateTestBuilder capex(BigDecimal capex) {
        this.capex = capex;
        return this;
    }

    public ScreenerCandidateTestBuilder revenue(BigDecimal revenue) {
        this.revenue = revenue;
        return this;
    }

    public ScreenerCandidateTestBuilder cash(BigDecimal cash) {
        this.cash = cash;
        return this;
    }

    public ScreenerCandidateTestBuilder longTermDebt(BigDecimal longTermDebt) {
        this.longTermDebt = longTermDebt;
        return this;
    }

    public ScreenerCandidateTestBuilder sharesOutstanding(BigDecimal sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
        return this;
    }

    public ScreenerCandidateTestBuilder operatingCashFlowPrior(BigDecimal operatingCashFlowPrior) {
        this.operatingCashFlowPrior = operatingCashFlowPrior;
        return this;
    }

    public ScreenerCandidateTestBuilder capexPrior(BigDecimal capexPrior) {
        this.capexPrior = capexPrior;
        return this;
    }

    public ScreenerCandidateTestBuilder netIncomePrior(BigDecimal netIncomePrior) {
        this.netIncomePrior = netIncomePrior;
        return this;
    }

    public ScreenerCandidateTestBuilder eps(BigDecimal eps) {
        this.eps = eps;
        return this;
    }

    public ScreenerCandidateTestBuilder bvps(BigDecimal bvps) {
        this.bvps = bvps;
        return this;
    }

    public ScreenerCandidateTestBuilder epsGrowthRate(BigDecimal epsGrowthRate) {
        this.epsGrowthRate = epsGrowthRate;
        return this;
    }

    public ScreenerCandidateTestBuilder qualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
        return this;
    }

    public ScreenerCandidateTestBuilder passesHardFilter(boolean passesHardFilter) {
        this.passesHardFilter = passesHardFilter;
        return this;
    }

    public ScreenerCandidateTestBuilder updatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public ScreenerCandidate build() {
        return new ScreenerCandidate(
            cik, name, periodEnd, adsh,
            assets, liabilities, stockholdersEquity, netIncome, operatingIncome, operatingCashFlow,
            capex, revenue, cash, longTermDebt, sharesOutstanding,
            operatingCashFlowPrior, capexPrior, netIncomePrior,
            eps, bvps, epsGrowthRate, qualityScore,
            passesHardFilter, updatedAt
        );
    }
}
