package dev.akbayin.fametrics.dto;

import java.util.List;

public record SummaryResponse(List<MetricResponse> metrics) {
}
