package dev.akbayin.fametrics.sec;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A cached, periodically-refreshed CIK-to-ticker mapping. Tickers change
 * rarely, so this is loaded once at startup ({@link PostConstruct}, which
 * blocks readiness until the first fetch succeeds — better than serving
 * requests for a few seconds with an empty map and every candidate
 * dropping) and re-fetched daily thereafter ({@link Scheduled}, with an
 * initialDelay equal to the refresh interval so it doesn't immediately
 * re-fetch what PostConstruct just fetched).
 */
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
        Map<String, SecTickerEntry> entries = client.fetchTickerEntries();

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
