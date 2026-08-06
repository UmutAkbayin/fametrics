package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PsRatioRequest(
    @NotNull @Positive BigDecimal marketCap,
    @NotNull @Positive BigDecimal totalRevenue
) {
}
