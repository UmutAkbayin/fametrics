package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LynchFairValueRequest(
    @NotNull @Positive BigDecimal eps,
    @NotNull BigDecimal epsGrowthRate,
    @Positive BigDecimal sharePrice
    ) {
}
