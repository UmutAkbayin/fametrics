package dev.akbayin.fametrics.sec;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A cached, periodically-refreshed CIK-to-ticker mapping. Tickers change
 * rarely, so this is fetched eagerly at startup ({@link PostConstruct}) and
 * re-fetched daily thereafter ({@link Scheduled}, with an initialDelay equal
 * to the refresh interval so it doesn't immediately re-fetch what
 * PostConstruct just fetched).
 * <p>
 * A failure here (SEC unreachable, a misconfigured User-Agent, ...) must
 * never crash application startup or wipe out a previously-good cache — the
 * rest of core, including the pre-existing metrics engine, has nothing to
 * do with this feature and must keep working regardless. On failure the
 * ranking feature just degrades (every ticker lookup misses) until the next
 * successful refresh, rather than the whole application going down.
 */
@Slf4j
@Component
public class CikTickerLookup {

    private static final long REFRESH_INTERVAL_HOURS = 24;

    private final SecTickerClient client;

    // volatile + wholesale replacement with an immutable snapshot is safe
    // publication without locking: a reader always sees either the old,
    // fully-built map or the new one, never a partially-built one.
    private volatile Map<Long, String> cikToTicker = Map.of();

    public CikTickerLookup(SecTickerClient client) {
        this.client = client;
    }

    @PostConstruct
    @Scheduled(initialDelay = REFRESH_INTERVAL_HOURS, fixedRate = REFRESH_INTERVAL_HOURS, timeUnit = TimeUnit.HOURS)
    void refresh() {
        Map<String, SecTickerEntry> entries;
        try {
            entries = client.fetchTickerEntries();
        } catch (RuntimeException e) {
            log.warn("failed to fetch SEC ticker file; keeping the existing CIK-to-ticker mapping", e);
            return;
        }

        // First entry per CIK wins: SEC consistently lists the primary/
        // common-class listing before preferred shares or a second share
        // class for the same company (e.g. GOOGL before GOOG, BRK-B before
        // BRK-A, JPM before its nine JPM-P* preferred variants) — confirmed
        // against the live file, not assumed.
        Map<Long, String> next = new LinkedHashMap<>();
        for (SecTickerEntry entry : entries.values()) {
            next.putIfAbsent(entry.cikStr(), entry.ticker());
        }

        this.cikToTicker = Map.copyOf(next);
    }

    /**
     * The ticker for cik, or empty if SEC has no ticker on file for it — a
     * real, expected case (e.g. a filer with public debt but no public
     * equity, like a wholly-owned subsidiary), not an error condition.
     */
    public Optional<String> findTicker(long cik) {
        return Optional.ofNullable(cikToTicker.get(cik));
    }
}
