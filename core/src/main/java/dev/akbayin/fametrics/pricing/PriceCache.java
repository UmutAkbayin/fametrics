package dev.akbayin.fametrics.pricing;

import dev.akbayin.fametrics.fmp.FmpClient;
import dev.akbayin.fametrics.fmp.FmpProfile;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import dev.akbayin.fametrics.screener.ScreenerClient;
import dev.akbayin.fametrics.sec.CikTickerLookup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A cached, periodically-refreshed price snapshot for the screener's
 * candidates, keyed by CIK. FMP's free tier is 250 calls/day and doesn't
 * support batch quoting, so pricing ~200 candidates on every request isn't
 * viable — this refreshes once a day in the background instead, and reads
 * are always served from that snapshot. Prices are therefore "as of the
 * last refresh", not live; that's a deliberate trade for staying on a free
 * plan, not an oversight.
 * <p>
 * No {@link jakarta.annotation.PostConstruct}, unlike {@link CikTickerLookup}:
 * refreshing ~200 candidates against FMP sequentially can take tens of
 * seconds, and that isn't worth blocking application startup for. The cache
 * starts empty and is filled by the first scheduled run.
 * <p>
 * A run only counts as "successful" (and settles into the {@link
 * #REFRESH_INTERVAL} cadence) if it actually priced something, or the
 * screener genuinely had zero candidates to give it. Fetching candidates
 * successfully but pricing none of them — e.g. because {@link
 * CikTickerLookup}'s own mapping is currently empty (this is exactly the
 * cascading state observed live: SEC rate-limited the ticker lookup, which
 * meant every candidate here missed its ticker too) — is treated the same
 * as a failure, so this retries every {@link #RETRY_CHECK_INTERVAL_MINUTES}
 * rather than staying empty for a full day after the real upstream problem
 * has already cleared.
 */
@Slf4j
@Component
public class PriceCache {

    private static final Duration REFRESH_INTERVAL = Duration.ofHours(24);
    private static final long RETRY_CHECK_INTERVAL_MINUTES = 5;
    private static final int CANDIDATE_LIMIT = 200;

    private final ScreenerClient screenerClient;
    private final CikTickerLookup tickerLookup;
    private final FmpClient fmpClient;

    // volatile + wholesale replacement with an immutable snapshot: same
    // lock-free safe-publication pattern as CikTickerLookup.
    private volatile Map<Long, CachedPrice> prices = Map.of();
    private volatile Instant lastSuccessfulRefresh;

    public PriceCache(ScreenerClient screenerClient, CikTickerLookup tickerLookup, FmpClient fmpClient) {
        this.screenerClient = screenerClient;
        this.tickerLookup = tickerLookup;
        this.fmpClient = fmpClient;
    }

    @Scheduled(fixedRate = RETRY_CHECK_INTERVAL_MINUTES, timeUnit = TimeUnit.MINUTES)
    void refreshIfDue() {
        if (lastSuccessfulRefresh == null || Duration.between(lastSuccessfulRefresh, Instant.now()).compareTo(REFRESH_INTERVAL) >= 0) {
            refresh();
        }
    }

    void refresh() {
        List<ScreenerCandidate> candidates;
        try {
            candidates = screenerClient.fetchCandidates(CANDIDATE_LIMIT);
        } catch (RuntimeException e) {
            // The screener being briefly unreachable shouldn't wipe out an
            // otherwise-good cache — keep serving the last good prices.
            log.warn("failed to fetch candidates from the screener; keeping the existing price cache and retrying within {} minutes",
                RETRY_CHECK_INTERVAL_MINUTES, e);
            return;
        }

        Map<Long, CachedPrice> next = new LinkedHashMap<>();
        int noTicker = 0;
        int noPrice = 0;
        for (ScreenerCandidate candidate : candidates) {
            Optional<String> ticker = tickerLookup.findTicker(candidate.cik());
            if (ticker.isEmpty()) {
                noTicker++;
                continue;
            }

            try {
                Optional<FmpProfile> profile = fmpClient.fetchProfile(ticker.get());
                if (profile.isEmpty()) {
                    noPrice++;
                    continue;
                }
                next.put(candidate.cik(), new CachedPrice(ticker.get(), profile.get().price(), profile.get().marketCap(), Instant.now()));
            } catch (RuntimeException e) {
                // One bad symbol (rate limit, delisted, transient network
                // error) must not abort pricing for the rest of the batch.
                log.warn("failed to fetch FMP profile for {} (cik {}), skipping", ticker.get(), candidate.cik(), e);
                noPrice++;
            }
        }

        this.prices = Map.copyOf(next);
        log.info("refreshed price cache: priced={} noTicker={} noPrice={} total={}",
            next.size(), noTicker, noPrice, candidates.size());

        if (!next.isEmpty() || candidates.isEmpty()) {
            this.lastSuccessfulRefresh = Instant.now();
        } else {
            log.warn("priced 0 of {} candidates this refresh (likely the CIK-to-ticker mapping is currently empty/stale); retrying within {} minutes rather than waiting the full {}h",
                candidates.size(), RETRY_CHECK_INTERVAL_MINUTES, REFRESH_INTERVAL.toHours());
        }
    }

    public Optional<CachedPrice> findPrice(long cik) {
        return Optional.ofNullable(prices.get(cik));
    }
}
