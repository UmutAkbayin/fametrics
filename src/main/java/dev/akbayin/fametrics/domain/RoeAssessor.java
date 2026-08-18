package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.RoeRequest;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class RoeAssessor implements MetricAssessor {

    // Rough rule-of-thumb range, not sector- or rate-adjusted; arbitrary placeholder, not a researched value.
    private static final BigDecimal UNDERVALUED_THRESHOLD = new BigDecimal("15");
    private static final BigDecimal OVERVALUED_THRESHOLD = new BigDecimal("20");

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
            UNDERVALUED_THRESHOLD,
            OVERVALUED_THRESHOLD,
            "A ROE roughly between 15 and 20 is treated here as a broadly \"fair\" range. This ignores "
                + "sector, growth rate, and interest-rate context, so treat it as a rough signal, not a verdict."
        );

        Assessment assessment;
        if (roe.compareTo(UNDERVALUED_THRESHOLD) < 0) {
            assessment = new Assessment(Rating.FAVORABLE, "Low ROE");
        } else if (roe.compareTo(OVERVALUED_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.NEUTRAL, "Fair ROE");
        } else {
            assessment = new Assessment(Rating.UNFAVORABLE, "High ROE");
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

        var request = extractRequest(summaryRequest);
        if (request == null) {
            return Optional.empty();
        }

        var netIncome = request.netIncome();
        var totalEquity = request.totalEquity();

        if (netIncome == null || totalEquity == null || totalEquity.signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal result = netIncome.divide(totalEquity, 2, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    private RoeRequest extractRequest(SummaryRequest request) {
        if (request == null || request.capitalStructure() == null) {
            return null;
        }

        return new RoeRequest(
            request.capitalStructure().netIncome(),
            request.capitalStructure().totalEquity()
        );
    }
}
