package dev.akbayin.fametrics.dto;

import java.math.BigDecimal;

public record MarketData(
    BigDecimal sharePrice,
    BigDecimal eps,
    BigDecimal bvps
) {
}
