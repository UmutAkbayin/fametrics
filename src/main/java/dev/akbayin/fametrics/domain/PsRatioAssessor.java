package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.PsRatioRequest;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class PsRatioAssessor implements MetricAssessor {

    // Rough rule-of-thumb range, not sector- or rate-adjusted; arbitrary placeholder, not a researched value.
    private static final BigDecimal UNDERVALUED_THRESHOLD = new BigDecimal("1");
    private static final BigDecimal OVERVALUED_THRESHOLD = new BigDecimal("3");

    @Override
    public Metric metric() {
        return Metric.PS_RATIO;
    }

    @Override
    public String description() {
        return "The price-to-book (P/S) ratio is a financial metric that compares a company’s market value to its " +
            "earnings per share, and is often used to identify potentially undervalued stocks.";
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest request, BigDecimal psRatio) {
        var benchmark = new Benchmark(
            UNDERVALUED_THRESHOLD,
            OVERVALUED_THRESHOLD,
            "A P/S roughly between 1 and 3 is treated here as a broadly \"fair\" range. This ignores "
                + "sector, growth rate, and interest-rate context, so treat it as a rough signal, not a verdict."
        );

        Assessment assessment;
        if (psRatio.compareTo(UNDERVALUED_THRESHOLD) < 0) {
            assessment = new Assessment(Rating.FAVORABLE, "Low P/S");
        } else if (psRatio.compareTo(OVERVALUED_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.NEUTRAL, "Fair P/S");
        } else {
            assessment = new Assessment(Rating.UNFAVORABLE, "High P/S");
        }

        return new MetricEvaluation(assessment, benchmark);
    }

    @Override
    public String interpretation(SummaryRequest request, BigDecimal psRatio, Assessment assessment) {
        return "At " + psRatio + " the stock's P/S is considered " + assessment.label().toLowerCase() + ".";
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest summaryRequest) {
        var request = extractRequest(summaryRequest);
        if (request == null) {
            return Optional.empty();
        }

        var marketCap = request.marketCap();
        var totalRevenue = request.totalRevenue();

        if (anyNonPositive(marketCap, totalRevenue)) {
            return Optional.empty();
        }

        BigDecimal result = marketCap.divide(totalRevenue, 2, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    private PsRatioRequest extractRequest(SummaryRequest request) {
        if (request == null || request.marketData() == null) {
            return null;
        }

        return new PsRatioRequest(
            request.marketData().sharePrice(),
            request.marketData().eps()
        );
    }
}
