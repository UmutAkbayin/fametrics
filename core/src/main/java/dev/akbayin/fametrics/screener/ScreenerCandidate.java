package dev.akbayin.fametrics.screener;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * One row of the screener's {@code GET /candidates} response — see
 * screener/docs/openapi.yaml (Company schema) for the authoritative contract.
 * Field names here are camelCase; the screener's JSON is snake_case
 * (it mirrors the Postgres column names directly), so this type is bound
 * with {@link JsonNaming} rather than per-field {@code @JsonProperty}.
 * <p>
 * Every numeric field except {@code cik} can be null: the screener leaves a
 * field null whenever the source 10-K simply didn't report that XBRL tag,
 * rather than substituting zero.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScreenerCandidate(
    Long cik,
    String name,
    LocalDate periodEnd,
    String adsh,
    BigDecimal assets,
    BigDecimal liabilities,
    BigDecimal stockholdersEquity,
    BigDecimal netIncome,
    BigDecimal operatingIncome,
    BigDecimal operatingCashFlow,
    BigDecimal capex,
    BigDecimal revenue,
    BigDecimal cash,
    BigDecimal longTermDebt,
    BigDecimal sharesOutstanding,
    BigDecimal operatingCashFlowPrior,
    BigDecimal capexPrior,
    BigDecimal netIncomePrior,
    BigDecimal eps,
    BigDecimal bvps,
    BigDecimal epsGrowthRate,
    BigDecimal qualityScore,
    boolean passesHardFilter,
    OffsetDateTime updatedAt
) {
}
