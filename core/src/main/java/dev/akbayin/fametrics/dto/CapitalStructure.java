package dev.akbayin.fametrics.dto;

import java.math.BigDecimal;

public record CapitalStructure(
    BigDecimal totalLiabilities,
    BigDecimal totalEquity,
    BigDecimal netIncome
) {
}
