package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PbRatioRequest(
    @NotNull @Positive BigDecimal sharePrice,
    @NotNull @Positive BigDecimal bvps
) {
}
