package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PeTtmRequest(
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal sharePrice,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal eps
) {
}
