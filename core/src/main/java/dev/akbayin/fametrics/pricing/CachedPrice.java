package dev.akbayin.fametrics.pricing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A price snapshot as of asOf — never "live"; see {@link PriceCache}.
 */
public record CachedPrice(BigDecimal price, BigDecimal marketCap, Instant asOf) {
}
