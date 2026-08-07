package dev.akbayin.fametrics.dto;

import java.math.BigDecimal;

public record SummaryRequest(
    BigDecimal sharePrice,
    BigDecimal eps,
    BigDecimal bvps,
    BigDecimal marketCap,
    BigDecimal totalRevenue,
    BigDecimal epsGrowthRate,
    BigDecimal totalLiabilities,
    BigDecimal totalEquity,
    BigDecimal netIncome
    ) {
}
