package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class GrahamNumberAssessor implements MetricAssessor {

    private static final BigDecimal UNDERVALUED_THRESHOLD = new BigDecimal("0.90");
    private static final BigDecimal OVERVALUED_THRESHOLD = new BigDecimal("1.10");

    @Override
    public Metric metric() {
        return Metric.GRAHAM_NUMBER;
    }

    @Override
    public String description() {
        return "Benjamin Graham's estimate of a stock's maximum fair value, derived from "
            + "its earnings per share and book value per share.";
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest summaryRequest, BigDecimal grahamNumber) {
        Objects.requireNonNull(summaryRequest, "SummaryRequest must not be null");
        Objects.requireNonNull(grahamNumber, "Graham Number must not be null");

        var request = extractRequest(summaryRequest);
        var benchmark = new Benchmark(
            null,
            grahamNumber,
            "A share price at or below the Graham Number suggests the stock may be undervalued."
        );

        if (request == null) {
            return new MetricEvaluation(
                new Assessment(Rating.NOT_MEANINGFUL, "No market data provided for comparison"),
                benchmark
            );
        }

        var sharePrice = request.sharePrice();
        if (sharePrice == null) {
            return new MetricEvaluation(
                new Assessment(Rating.NOT_MEANINGFUL, "No share price provided for comparison"),
                benchmark
            );
        }

        var ratio = sharePrice.divide(grahamNumber, 2, RoundingMode.HALF_UP);
        Assessment assessment;
        if (ratio.compareTo(UNDERVALUED_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.FAVORABLE, "Undervalued");
        } else if (ratio.compareTo(OVERVALUED_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.NEUTRAL, "Fairly valued");
        } else {
            assessment = new Assessment(Rating.UNFAVORABLE, "Overvalued");
        }

        return new MetricEvaluation(assessment, benchmark);
    }

    @Override
    public String interpretation(SummaryRequest summaryRequest, BigDecimal grahamNumber, Assessment assessment) {
        Objects.requireNonNull(summaryRequest, "SummaryRequest must not be null");
        Objects.requireNonNull(grahamNumber, "Graham Number must not be null");

        var request = extractRequest(summaryRequest);

        if (request == null || request.sharePrice() == null) {
            return "Estimated intrinsic value is " + grahamNumber
                + ". Supply valid market data to compare it against.";
        }

        return "At a share price of " + request.sharePrice() + ", the stock looks " + assessment.label().toLowerCase()
            + " against an estimated intrinsic value of " + grahamNumber + ".";
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest summaryRequest) {
        Objects.requireNonNull(summaryRequest, "SummaryRequest must not be null");

        var request = extractRequest(summaryRequest);

        if (request == null) {
            return Optional.empty();
        }

        var eps = request.eps();
        var bvps = request.bvps();

        if (anyNonPositive(eps, bvps)) {
            return Optional.empty();
        }

        BigDecimal product = new BigDecimal("22.5")
            .multiply(eps)
            .multiply(bvps);

        BigDecimal result = product
            .sqrt(MathContext.DECIMAL64)
            .setScale(2, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    private GrahamRequest extractRequest(SummaryRequest request) {
        if (request.marketData() == null) {
            return null;
        }

        return new GrahamRequest(
            request.marketData().eps(),
            request.marketData().bvps(),
            request.marketData().sharePrice()
        );
    }
}