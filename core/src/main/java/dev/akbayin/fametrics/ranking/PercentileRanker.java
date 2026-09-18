package dev.akbayin.fametrics.ranking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Percentile-ranks a population of items by one numeric attribute, 0-100.
 * Same policy as the screener's own percentile ranking (see
 * screener/finance/candidates.go percentileRanks): an item whose value is
 * null is excluded from the population entirely, not penalized with a low
 * score — it simply gets no rank for that attribute.
 */
final class PercentileRanker {

    private PercentileRanker() {
    }

    enum Direction {
        /** e.g. ROE, discount to fair value — a bigger number is better. */
        HIGHER_IS_BETTER,
        /** e.g. PE, PB, PS, PEG — a smaller number is better. */
        LOWER_IS_BETTER
    }

    /**
     * @param items    the population to rank
     * @param idFn     a stable identifier per item (used as the result map's key)
     * @param valueFn  the attribute to rank on; items where this returns null
     *                 are excluded from both the ranking and the population
     * @param direction which direction counts as "better"
     * @return a map from idFn(item) to that item's percentile rank [0,100],
     *         containing only items whose valueFn was non-null
     */
    static <T, ID> Map<ID, BigDecimal> rank(List<T> items, Function<T, ID> idFn, Function<T, BigDecimal> valueFn, Direction direction) {
        record Point<ID>(ID id, BigDecimal value) {
        }

        List<Point<ID>> points = items.stream()
            .map(item -> new Point<>(idFn.apply(item), valueFn.apply(item)))
            .filter(p -> p.value() != null)
            .toList();

        Map<ID, BigDecimal> ranks = new LinkedHashMap<>();
        for (Point<ID> p : points) {
            long atOrBetter = points.stream()
                .filter(other -> direction == Direction.HIGHER_IS_BETTER
                    ? other.value().compareTo(p.value()) <= 0
                    : other.value().compareTo(p.value()) >= 0)
                .count();
            BigDecimal percentile = BigDecimal.valueOf(atOrBetter)
                .divide(BigDecimal.valueOf(points.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            ranks.put(p.id(), percentile);
        }
        return ranks;
    }
}
