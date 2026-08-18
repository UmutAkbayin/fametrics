package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.PbRatioRequest;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class PbRatioAssessor implements MetricAssessor {

    // Rough rule-of-thumb range, not sector- or rate-adjusted; arbitrary placeholder, not a researched value.
    private static final BigDecimal UNDERVALUED_THRESHOLD = new BigDecimal("1");
    private static final BigDecimal OVERVALUED_THRESHOLD = new BigDecimal("3");

    @Override
    public Metric metric() {
        return Metric.PB_RATIO;
    }

    @Override
    public String description() {
        return "The price-to-book (P/B) ratio is a financial metric that compares a company’s market value to its book " +
            "value, total assets minus total liabilities, and is often used to identify potentially undervalued stocks.";
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest request, BigDecimal pbRatio) {
        var benchmark = new Benchmark(
            UNDERVALUED_THRESHOLD,
            OVERVALUED_THRESHOLD,
            "A P/B roughly between 1 and 3 is treated here as a broadly \"fair\" range. This ignores "
                + "sector, growth rate, and interest-rate context, so treat it as a rough signal, not a verdict."
        );

        Assessment assessment;
        if (pbRatio.compareTo(UNDERVALUED_THRESHOLD) < 0) {
            assessment = new Assessment(Rating.FAVORABLE, "Low P/B");
        } else if (pbRatio.compareTo(OVERVALUED_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.NEUTRAL, "Fair P/B");
        } else {
            assessment = new Assessment(Rating.UNFAVORABLE, "High P/B");
        }

        return new MetricEvaluation(assessment, benchmark);
    }

    @Override
    public String interpretation(SummaryRequest request, BigDecimal pbRatio, Assessment assessment) {
        return "At " + pbRatio + " the stock's P/B is considered " + assessment.label().toLowerCase() + ".";
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest summaryRequest) {
        PbRatioRequest request = extractRequest(summaryRequest);
        if (request == null) {
            return Optional.empty();
        }

        var sharePrice = request.sharePrice();
        var bvps = request.bvps();

        if (anyNonPositive(sharePrice, bvps)) {
            return Optional.empty();
        }

        BigDecimal result = sharePrice.divide(bvps, 2, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    private PbRatioRequest extractRequest(SummaryRequest request) {
        if (request == null || request.marketData() == null) {
            return null;
        }

        return new PbRatioRequest(
            request.marketData().sharePrice(),
            request.marketData().bvps()
        );
    }
}
