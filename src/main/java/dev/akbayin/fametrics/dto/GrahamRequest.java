package dev.akbayin.fametrics.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GrahamRequest(
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal eps,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal bvps
) {
}
