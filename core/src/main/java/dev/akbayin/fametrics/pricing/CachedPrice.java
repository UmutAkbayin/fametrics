package dev.akbayin.fametrics.pricing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A price snapshot as of asOf — never "live"; see {@link PriceCache}.
 * Carries the ticker it was resolved to, since nothing downstream otherwise
 * retains it once the CIK-to-ticker lookup has happened here.
 */
public record CachedPrice(String ticker, BigDecimal price, BigDecimal marketCap, Instant asOf) {
}
