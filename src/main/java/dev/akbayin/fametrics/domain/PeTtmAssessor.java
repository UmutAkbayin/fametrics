package dev.akbayin.fametrics.domain;

import dev.akbayin.fametrics.dto.PeTtmRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PeTtmAssessor implements MetricAssessor<PeTtmRequest> {

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
    public MetricEvaluation evaluate(PeTtmRequest request, BigDecimal peTtm) {
        var benchmark = new Benchmark(
            UNDERVALUED_THRESHOLD,
            OVERVALUED_THRESHOLD,
            "A P/E roughly between 15 and 25 is treated here as a broadly \"fair\" range. This ignores "
                + "sector, growth rate, and interest-rate context, so treat it as a rough signal, not a verdict."
        );

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
    public String interpretation(PeTtmRequest request, BigDecimal peTtm, Assessment assessment) {
        return "At " + peTtm + " trailing earnings, the stock's P/E is considered "
            + assessment.label().toLowerCase() + ".";
    }
}