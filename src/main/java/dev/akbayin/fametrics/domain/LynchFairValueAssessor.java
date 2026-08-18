package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class LynchFairValueAssessor implements MetricAssessor {

    private static final BigDecimal UNDERVALUED_THRESHOLD = new BigDecimal("0.90");
    private static final BigDecimal OVERVALUED_THRESHOLD = new BigDecimal("1.10");

    @Override
    public Metric metric() {
        return Metric.LYNCH_FAIR_VALUE;
    }

    @Override
    public String description() {
        return "Peter Lynch's rule of thumb: a stock is fairly valued when its P/E ratio equals its "
            + "long-term earnings growth rate (e.g. 15% growth implies a fair P/E of 15). Fair value price "
            + "is estimated by multiplying EPS by that growth rate.";
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest request, BigDecimal lynchFairValue) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(lynchFairValue, "Lynch Fair Value must not be null");

        var benchmark = new Benchmark(
            null,
            lynchFairValue,
            "A share price at or below the Lynch Fair Value suggests the stock may be undervalued "
                + "relative to its earnings growth rate."
        );

        if (request.marketData() == null || request.fundamentalData() == null) {
            return new MetricEvaluation(
                new Assessment(Rating.NOT_MEANINGFUL, "No market data or fundamental data provided for comparison"),
                benchmark
            );
        }

        var sharePrice = request.marketData().sharePrice();
        if (sharePrice == null) {
            return new MetricEvaluation(
                new Assessment(Rating.NOT_MEANINGFUL, "No share price provided for comparison"),
                benchmark
            );
        }

        var ratio = sharePrice.divide(lynchFairValue, 4, RoundingMode.HALF_UP);
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
    public String interpretation(SummaryRequest request, BigDecimal lynchFairValue, Assessment assessment) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(lynchFairValue, "Lynch Fair Value must not be null");

        if (request.marketData() == null || request.marketData().sharePrice() == null || request.fundamentalData() == null) {
            return "Estimated fair value is " + lynchFairValue + ". Supply a share price to compare it against.";
        }

        return "At a share price of " + request.marketData().sharePrice() + ", the stock looks " + assessment.label().toLowerCase()
            + " against an estimated fair value of " + lynchFairValue + ".";
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest request) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");


        if (request.marketData() == null || request.fundamentalData() == null) {
            return Optional.empty();
        }

        var eps = request.marketData().eps();
        var epsGrowthRate = request.fundamentalData().epsGrowthRate();

        if (eps == null || eps.signum() <= 0 || epsGrowthRate == null) {
            return Optional.empty();
        }

        BigDecimal result = eps
            .multiply(epsGrowthRate.multiply(new BigDecimal("100")))
            .setScale(2, RoundingMode.HALF_UP);

        return Optional.of(result);
    }
}
