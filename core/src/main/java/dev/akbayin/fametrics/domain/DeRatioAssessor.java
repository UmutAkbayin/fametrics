package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class DeRatioAssessor implements MetricAssessor {

    // Rough rule-of-thumb range, not sector- or rate-adjusted; arbitrary placeholder, not a researched value.
    private static final BigDecimal UNDERVALUED_THRESHOLD = new BigDecimal("1.00");
    private static final BigDecimal OVERVALUED_THRESHOLD = new BigDecimal("2.00");

    @Override
    public Metric metric() {
        return Metric.DE_RATIO;
    }

    @Override
    public String description() {
        return "The debt-to-equity (D/E) ratio is a calculation of a company’s total liabilities and shareholder " +
            "equity that evaluates its reliance on debt.";
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest request, BigDecimal deRatio) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(deRatio, "DeRatio must not be null");

        var benchmark = new Benchmark(
            UNDERVALUED_THRESHOLD,
            OVERVALUED_THRESHOLD,
            "A D/E roughly between 1 and 2 is treated here as a broadly \"fair\" range. This ignores "
                + "sector, growth rate, and interest-rate context, so treat it as a rough signal, not a verdict."
        );

        Assessment assessment;
        if (deRatio.compareTo(UNDERVALUED_THRESHOLD) < 0) {
            assessment = new Assessment(Rating.FAVORABLE, "Low D/E");
        } else if (deRatio.compareTo(OVERVALUED_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.NEUTRAL, "Fair D/E");
        } else {
            assessment = new Assessment(Rating.UNFAVORABLE, "High D/E");
        }

        return new MetricEvaluation(assessment, benchmark);
    }

    @Override
    public String interpretation(SummaryRequest request, BigDecimal deRatio, Assessment assessment) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(deRatio, "DeRatio must not be null");

        return "At " + deRatio + " the stock's D/E is considered " + assessment.label().toLowerCase() + ".";
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest summaryRequest) {
        Objects.requireNonNull(summaryRequest, "SummaryRequest must not be null");

        var request = summaryRequest.capitalStructure();

        var totalLiabilities = request.totalLiabilities();
        var totalEquity = request.totalEquity();

        if (totalLiabilities == null || totalLiabilities.signum() < 0
            || totalEquity == null || totalEquity.signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal result = totalLiabilities.divide(totalEquity, 2, RoundingMode.HALF_UP);

        return Optional.of(result);
    }
}
