package dev.akbayin.fametrics.dto;

import java.math.BigDecimal;
import java.util.List;

public record SummaryResponse(List<MetricResponse> metrics) {
}
