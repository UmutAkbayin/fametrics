package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RoeRequest(
    @NotNull BigDecimal netIncome,
    @NotNull @Positive BigDecimal totalEquity
) {
}
