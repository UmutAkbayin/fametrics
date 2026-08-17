package dev.akbayin.fametrics.dto;

import java.math.BigDecimal;

public record SummaryRequest(
    MarketData marketData,
    FundamentalData fundamentalData,
    CapitalStructure capitalStructure
    ) {
}
