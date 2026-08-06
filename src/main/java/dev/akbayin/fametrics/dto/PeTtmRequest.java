package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PeTtmRequest(
    @NotNull @Positive BigDecimal sharePrice,
    @NotNull @Positive BigDecimal eps
) {
}
