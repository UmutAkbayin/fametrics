package dev.akbayin.fametrics.dto;

public record SummaryRequest(
    MarketData marketData,
    FundamentalData fundamentalData,
    CapitalStructure capitalStructure
    ) {
}
