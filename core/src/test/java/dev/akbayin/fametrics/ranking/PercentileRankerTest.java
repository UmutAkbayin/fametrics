package dev.akbayin.fametrics.ranking;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PercentileRankerTest {

    private record Item(String id, BigDecimal value) {
    }

    @Test
    void rank_higherIsBetter_bestValueGetsHundred() {
        List<Item> items = List.of(
            new Item("low", new BigDecimal("5")),
            new Item("mid", new BigDecimal("10")),
            new Item("high", new BigDecimal("15"))
        );

        Map<String, BigDecimal> ranks = PercentileRanker.rank(items, Item::id, Item::value, PercentileRanker.Direction.HIGHER_IS_BETTER);

        assertThat(ranks.get("high")).isEqualByComparingTo("100.0000");
        assertThat(ranks.get("mid")).isEqualByComparingTo("66.6700");
        assertThat(ranks.get("low")).isEqualByComparingTo("33.3300");
    }

    @Test
    void rank_lowerIsBetter_cheapestValueGetsHundred() {
        List<Item> items = List.of(
            new Item("cheap", new BigDecimal("5")),
            new Item("mid", new BigDecimal("10")),
            new Item("expensive", new BigDecimal("15"))
        );

        Map<String, BigDecimal> ranks = PercentileRanker.rank(items, Item::id, Item::value, PercentileRanker.Direction.LOWER_IS_BETTER);

        assertThat(ranks.get("cheap")).isEqualByComparingTo("100.0000");
        assertThat(ranks.get("mid")).isEqualByComparingTo("66.6700");
        assertThat(ranks.get("expensive")).isEqualByComparingTo("33.3300");
    }

    @Test
    void rank_excludesNullValuesFromBothResultAndPopulation() {
        List<Item> items = List.of(
            new Item("has-value-a", new BigDecimal("1")),
            new Item("missing", null),
            new Item("has-value-b", new BigDecimal("2"))
        );

        Map<String, BigDecimal> ranks = PercentileRanker.rank(items, Item::id, Item::value, PercentileRanker.Direction.HIGHER_IS_BETTER);

        assertThat(ranks).doesNotContainKey("missing");
        // population size used for the percentile math is 2, not 3 — the
        // missing item doesn't dilute the population
        assertThat(ranks.get("has-value-a")).isEqualByComparingTo("50.0000");
        assertThat(ranks.get("has-value-b")).isEqualByComparingTo("100.0000");
    }

    @Test
    void rank_emptyPopulation_returnsEmptyMap() {
        Map<String, BigDecimal> ranks = PercentileRanker.rank(List.<Item>of(), Item::id, Item::value, PercentileRanker.Direction.HIGHER_IS_BETTER);

        assertThat(ranks).isEmpty();
    }
}
