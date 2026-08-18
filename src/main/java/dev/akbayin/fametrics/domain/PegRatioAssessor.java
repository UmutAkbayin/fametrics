package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.SummaryRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class PegRatioAssessor implements MetricAssessor {

    // Rough rule-of-thumb range, not sector- or rate-adjusted; arbitrary placeholder, not a researched value.
    private static final BigDecimal UNDERVALUED_THRESHOLD = new BigDecimal("1");
    private static final BigDecimal OVERVALUED_THRESHOLD = new BigDecimal("1.5");

    @Override
    public Metric metric() {
        return Metric.PEG_RATIO;
    }

    @Override
    public String description() {
        return "The price/earnings-to-growth (PEG) ratio is a valuation metric that adjusts traditional earnings " +
            "multiples by factoring in a company's expected growth rate.";
    }

    @Override
    public MetricEvaluation evaluate(SummaryRequest request, BigDecimal pegRatio) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(pegRatio, "PegRatio must not be null");

        var benchmark = new Benchmark(
            UNDERVALUED_THRESHOLD,
            OVERVALUED_THRESHOLD,
            "A PEG roughly between 1 and 1.5 is treated here as a broadly \"fair\" range. This ignores "
                + "sector, growth rate, and interest-rate context, so treat it as a rough signal, not a verdict."
        );

        Assessment assessment;
        if (pegRatio.compareTo(UNDERVALUED_THRESHOLD) < 0) {
            assessment = new Assessment(Rating.FAVORABLE, "Low PEG");
        } else if (pegRatio.compareTo(OVERVALUED_THRESHOLD) <= 0) {
            assessment = new Assessment(Rating.NEUTRAL, "Fair PEG");
        } else {
            assessment = new Assessment(Rating.UNFAVORABLE, "High PEG");
        }

        return new MetricEvaluation(assessment, benchmark);
    }

    @Override
    public String interpretation(SummaryRequest request, BigDecimal pegRatio, Assessment assessment) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");
        Objects.requireNonNull(pegRatio, "PegRatio must not be null");

        return "At " + pegRatio + " the stock's PEG is considered " + assessment.label().toLowerCase() + ".";
    }

    @Override
    public Optional<BigDecimal> calculate(SummaryRequest request) {
        Objects.requireNonNull(request, "SummaryRequest must not be null");

        if (request.fundamentalData() == null || request.marketData() == null) {
            return Optional.empty();
        }

        var epsGrowthRate = request.fundamentalData().epsGrowthRate();

        if (epsGrowthRate == null || epsGrowthRate.signum() <= 0) {
            return Optional.empty();
        }

        if (anyNonPositive(request.marketData().sharePrice(), request.marketData().eps())) {
            return Optional.empty();
        }

        var peRatio = request.marketData().sharePrice().divide(request.marketData().eps(), 2, RoundingMode.HALF_UP);

        return Optional.of(
            peRatio.divide(epsGrowthRate.multiply(new BigDecimal("100")), 2, RoundingMode.HALF_UP)
        );
    }
}
