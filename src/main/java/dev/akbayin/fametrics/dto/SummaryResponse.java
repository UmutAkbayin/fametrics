package dev.akbayin.fametrics.dto;

import java.math.BigDecimal;

public record SummaryResponse(
    BigDecimal peTtm,
    BigDecimal pbRatio,
    BigDecimal psRatio,
    BigDecimal pegRatio,
    BigDecimal deRatio,
    BigDecimal roeRatio,
    BigDecimal grahamNumber,
    BigDecimal lynchFairValue
) {
}
