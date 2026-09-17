package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class RoeAssessor implements MetricAssessor {

    // Rough rule-of-thumb range, not sector- or rate-adjusted; arbitrary placeholder, not a researched value.
    // Named LOW/HIGH rather than UNDERVALUED/OVERVALUED: ROE is a profitability ratio, not a valuation
    // multiple, so it says nothing about whether the stock's price is cheap or expensive.
    private static final BigDecimal LOW_ROE_THRESHOLD = new BigDecimal("15");
    private static final BigDecimal HIGH_ROE_THRESHOLD = new BigDecimal("20");

    @Override
    public Metric metric() {
        return Metric.ROE;
    }

    @Override
    public String description() {
        return "Return on equity (ROE) is a financial performance ratio that measures how effectively a company uses " +
            "shareholders’ equity to generate net income.";
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest request, BigDecimal roe) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(roe, "ROE must not be null");

        var benchmark = new Benchmark(
            LOW_ROE_THRESHOLD,
            HIGH_ROE_THRESHOLD,
            "A ROE roughly between 15 and 20 is treated here as a broadly \"fair\" range. This ignores "
                + "sector, growth rate, and interest-rate context, so treat it as a rough signal, not a verdict."
        );

        // Unlike a valuation multiple (P/E, P/B, D/E), where low is favorable, ROE is a profitability
        // ratio: a higher ROE means the company generates more profit per dollar of shareholder equity,
        // which is a quality signal, not a red flag.
        Assessment assessment;
        if (roe.compareTo(LOW_ROE_THRESHOLD) < 0) {
            assessment = new Assessment(Rating.UNFAVORABLE, "Low ROE");
        } else if (roe.compareTo(HIGH_ROE_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.NEUTRAL, "Fair ROE");
        } else {
            assessment = new Assessment(Rating.FAVORABLE, "High ROE");
        }

        return new MetricEvaluation(assessment, benchmark);
    }

    @Override
    public String interpretation(SummaryRequest request, BigDecimal roe, Assessment assessment) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(roe, "ROE must not be null");

        return "At " + roe + " the stock's ROE is considered " + assessment.label().toLowerCase() + ".";
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest summaryRequest) {
        Objects.requireNonNull(summaryRequest, "SummaryRequest must not be null");

        var request = summaryRequest.capitalStructure();
        if (request == null) {
            return Optional.empty();
        }

        var netIncome = request.netIncome();
        var totalEquity = request.totalEquity();

        if (netIncome == null || totalEquity == null || totalEquity.signum() <= 0) {
            return Optional.empty();
        }

        // evaluate()'s thresholds (15, 20) are percentage-scale, so the raw
        // fraction from the division must be scaled up to match — dividing
        // to 4 places first keeps enough precision that the *100 doesn't
        // lose a digit before the final rounding.
        BigDecimal result = netIncome.divide(totalEquity, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);

        return Optional.of(result);
    }
}
