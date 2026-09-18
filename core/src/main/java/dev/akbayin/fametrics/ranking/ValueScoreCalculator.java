package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.domain.Metric;
import dev.akbayin.fametrics.dto.MetricResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Computes core's price-dependent Value Score for a batch of assessed
 * candidates: a percentile rank per candidate on PE, PB, PS, PEG (lower is
 * better) and discount-to-fair-value (higher is better), averaged into one
 * composite. Percentiles are computed once across the whole batch, so this
 * only makes sense called with the full candidate pool at once, not one
 * candidate at a time — unlike {@link CandidateAssessor}, which is
 * per-candidate because it doesn't need the rest of the pool.
 */
@Component
public class ValueScoreCalculator {

    public List<RankedCandidate> compute(List<AssessedCandidate> candidates) {
        Map<Long, BigDecimal> peRanks = PercentileRanker.rank(candidates, this::cik, this::peValue, PercentileRanker.Direction.LOWER_IS_BETTER);
        Map<Long, BigDecimal> pbRanks = PercentileRanker.rank(candidates, this::cik, this::pbValue, PercentileRanker.Direction.LOWER_IS_BETTER);
        Map<Long, BigDecimal> psRanks = PercentileRanker.rank(candidates, this::cik, this::psValue, PercentileRanker.Direction.LOWER_IS_BETTER);
        Map<Long, BigDecimal> pegRanks = PercentileRanker.rank(candidates, this::cik, this::pegValue, PercentileRanker.Direction.LOWER_IS_BETTER);
        Map<Long, BigDecimal> discountRanks = PercentileRanker.rank(candidates, this::cik, this::discountToFairValue, PercentileRanker.Direction.HIGHER_IS_BETTER);

        List<RankedCandidate> result = new ArrayList<>();
        for (AssessedCandidate candidate : candidates) {
            Long cik = cik(candidate);
            BigDecimal pe = peRanks.get(cik);
            BigDecimal pb = pbRanks.get(cik);
            BigDecimal ps = psRanks.get(cik);
            BigDecimal peg = pegRanks.get(cik);
            BigDecimal discount = discountRanks.get(cik);

            BigDecimal composite = average(pe, pb, ps, peg, discount);
            result.add(new RankedCandidate(candidate, new ValueScore(pe, pb, ps, peg, discount, composite)));
        }
        return result;
    }

    private Long cik(AssessedCandidate candidate) {
        return candidate.candidate().cik();
    }

    private BigDecimal peValue(AssessedCandidate candidate) {
        return metricValue(candidate, Metric.PE_TTM);
    }

    private BigDecimal pbValue(AssessedCandidate candidate) {
        return metricValue(candidate, Metric.PB_RATIO);
    }

    private BigDecimal psValue(AssessedCandidate candidate) {
        return metricValue(candidate, Metric.PS_RATIO);
    }

    private BigDecimal pegValue(AssessedCandidate candidate) {
        return metricValue(candidate, Metric.PEG_RATIO);
    }

    /**
     * The average of the Graham- and Lynch-derived discounts, whichever are
     * available: {@code 1 - price/fairValue}, positive meaning the price is
     * below the estimated fair value (undervalued).
     */
    private BigDecimal discountToFairValue(AssessedCandidate candidate) {
        BigDecimal price = candidate.price().price();
        BigDecimal grahamDiscount = discountFrom(price, metricValue(candidate, Metric.GRAHAM_NUMBER));
        BigDecimal lynchDiscount = discountFrom(price, metricValue(candidate, Metric.LYNCH_FAIR_VALUE));
        return average(grahamDiscount, lynchDiscount);
    }

    private BigDecimal discountFrom(BigDecimal price, BigDecimal fairValue) {
        // fairValue <= 0 is a real possibility for Lynch Fair Value
        // specifically (eps * epsGrowthRate*100 goes negative when growth
        // is negative — that formula's own known quirk, not something to
        // silently work around here). Dividing by a non-positive fair value
        // would flip the discount's sign into something that looks
        // favorable for a distressed case, same class of bug as the
        // screener's ratio()/positiveRatio() distinction — so it's excluded
        // rather than computed.
        if (price == null || fairValue == null || fairValue.signum() <= 0) {
            return null;
        }
        return BigDecimal.ONE.subtract(price.divide(fairValue, 6, RoundingMode.HALF_UP));
    }

    private BigDecimal metricValue(AssessedCandidate candidate, Metric metric) {
        return candidate.summary().metrics().stream()
            .filter(m -> m.metric() == metric)
            .map(MetricResponse::value)
            .findFirst()
            .orElse(null);
    }

    private BigDecimal average(BigDecimal... values) {
        List<BigDecimal> present = Stream.of(values).filter(Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return present.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }
}
