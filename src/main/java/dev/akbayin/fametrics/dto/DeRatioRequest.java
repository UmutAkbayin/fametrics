package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record DeRatioRequest(
    @NotNull @PositiveOrZero BigDecimal totalLiabilities,
    @NotNull @Positive BigDecimal totalEquity
    ) {
}
