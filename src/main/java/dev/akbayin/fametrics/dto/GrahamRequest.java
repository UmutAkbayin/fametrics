package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record GrahamRequest(
    @NotNull @Positive BigDecimal eps,
    @NotNull @Positive BigDecimal bvps
) {
}
