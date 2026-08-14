package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.GrahamRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GrahamNumberAssessor implements MetricAssessor<GrahamRequest> {

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
    public MetricEvaluation evaluate(GrahamRequest request, BigDecimal grahamNumber) {
        var benchmark = new Benchmark(
            null,
            grahamNumber,
            "A share price at or below the Graham Number suggests the stock may be undervalued."
        );

        var sharePrice = request.sharePrice();
        if (sharePrice == null) {
            return new MetricEvaluation(
                new Assessment(Rating.NOT_MEANINGFUL, "No share price provided for comparison"),
                benchmark
            );
        }

        var ratio = sharePrice.divide(grahamNumber, 4, RoundingMode.HALF_UP);
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

    public String interpretation(BigDecimal grahamNumber, BigDecimal sharePrice, String label) {
        if (sharePrice == null) {
            return "Estimated intrinsic value is " + grahamNumber
                + ". Supply a share price to compare it against.";
        }

        return "At a share price of " + sharePrice + ", the stock looks " + label.toLowerCase()
            + " against an estimated intrinsic value of " + grahamNumber + ".";
    }
}