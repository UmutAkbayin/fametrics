package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.PeTtmRequest;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class PeTtmAssessor implements MetricAssessor {

    // Rough rule-of-thumb range, not sector- or rate-adjusted; arbitrary placeholder, not a researched value.
    private static final BigDecimal UNDERVALUED_THRESHOLD = new BigDecimal("15");
    private static final BigDecimal OVERVALUED_THRESHOLD = new BigDecimal("25");

    @Override
    public Metric metric() {
        return Metric.PE_TTM;
    }

    @Override
    public String description() {
        return "Price-to-Earnings (trailing twelve months): how many times trailing annual earnings "
            + "the market is currently paying for one share.";
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest request, BigDecimal peTtm) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(peTtm, "PeTtm must not be null");

        var benchmark = new Benchmark(
            UNDERVALUED_THRESHOLD,
            OVERVALUED_THRESHOLD,
            "A P/E roughly between 15 and 25 is treated here as a broadly \"fair\" range. This ignores "
                + "sector, growth rate, and interest-rate context, so treat it as a rough signal, not a verdict."
        );

        if (request == null) {
            return new MetricEvaluation(
                new Assessment(Rating.NOT_MEANINGFUL, "No market data provided for comparison"),
                benchmark
            );
        }

        Assessment assessment;
        if (peTtm.compareTo(UNDERVALUED_THRESHOLD) < 0) {
            assessment = new Assessment(Rating.FAVORABLE, "Low P/E");
        } else if (peTtm.compareTo(OVERVALUED_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.NEUTRAL, "Fair P/E");
        } else {
            assessment = new Assessment(Rating.UNFAVORABLE, "High P/E");
        }

        return new MetricEvaluation(assessment, benchmark);
    }

    @Override
    public String interpretation(SummaryRequest request, BigDecimal peTtm, Assessment assessment) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(peTtm, "PeTtm must not be null");

        return "At " + peTtm + " trailing earnings, the stock's P/E is considered "
            + assessment.label().toLowerCase() + ".";
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest summaryRequest) {
        Objects.requireNonNull(summaryRequest, "SummaryRequest must not be null");

        var request = extractRequest(summaryRequest);
        if (request == null) {
            return Optional.empty();
        }

        var sharePrice = request.sharePrice();
        var eps = request.eps();

        if (anyNonPositive(sharePrice, eps)) {
            return Optional.empty();
        }

        BigDecimal result = sharePrice.divide(eps, 2, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    private PeTtmRequest extractRequest(SummaryRequest request) {
        if (request.marketData() == null) {
            return null;
        }

        return new PeTtmRequest(
            request.marketData().sharePrice(),
            request.marketData().eps()
        );
    }
}