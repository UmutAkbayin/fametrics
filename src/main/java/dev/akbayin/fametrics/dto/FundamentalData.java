package dev.akbayin.fametrics.dto;

import java.math.BigDecimal;

public record FundamentalData(
    BigDecimal marketCap,
    BigDecimal totalRevenue,
    BigDecimal epsGrowthRate
) {
}
