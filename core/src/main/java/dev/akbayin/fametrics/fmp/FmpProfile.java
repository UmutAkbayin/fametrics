package dev.akbayin.fametrics.fmp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * The handful of fields we actually use from FMP's {@code /stable/profile}
 * response — that endpoint returns ~25 fields (description, CEO, address,
 * ...) we have no use for. {@code ignoreUnknown} is explicit rather than
 * relying on Spring's Jackson defaults: a third-party API can add fields at
 * any time without notice, and that must never break deserialization here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FmpProfile(String symbol, BigDecimal price, BigDecimal marketCap) {
}
