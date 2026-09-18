package dev.akbayin.fametrics.sec;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A cached, periodically-refreshed CIK-to-ticker mapping. Tickers change
 * rarely, so a successful refresh is only repeated every {@link
 * #REFRESH_INTERVAL}. But a *failed* attempt (SEC unreachable, SEC's own
 * rate limiting — a real, observed failure mode, not hypothetical) is
 * retried every {@link #RETRY_CHECK_INTERVAL_MINUTES} instead of waiting
 * out the full interval regardless of outcome: a naive fixed 24h retry
 * meant a transient failure (typically clears in minutes) left the whole
 * ranking feature dead for a day.
 * <p>
 * A failure here must never crash application startup or wipe out a
 * previously-good cache — the rest of core, including the pre-existing
 * metrics engine, has nothing to do with this feature and must keep
 * working regardless. On failure the ranking feature just degrades (every
 * ticker lookup misses) until the next successful refresh.
 */
@Slf4j
@Component
public class CikTickerLookup {

    private static final Duration REFRESH_INTERVAL = Duration.ofHours(24);
    private static final long RETRY_CHECK_INTERVAL_MINUTES = 5;

    private final SecTickerClient client;

    // volatile + wholesale replacement with an immutable snapshot is safe
    // publication without locking: a reader always sees either the old,
    // fully-built map or the new one, never a partially-built one.
    private volatile Map<Long, String> cikToTicker = Map.of();
    private volatile Instant lastSuccessfulRefresh;

    public CikTickerLookup(SecTickerClient client) {
        this.client = client;
    }

    @PostConstruct
    void initialize() {
        refresh();
    }

    /**
     * Checked every {@link #RETRY_CHECK_INTERVAL_MINUTES}, but only actually
     * refreshes if there's never been a successful refresh, or the last one
     * was more than {@link #REFRESH_INTERVAL} ago — so a healthy cache stays
     * on its normal daily cadence, while a broken one retries promptly.
     */
    @Scheduled(fixedRate = RETRY_CHECK_INTERVAL_MINUTES, timeUnit = TimeUnit.MINUTES)
    void refreshIfDue() {
        if (lastSuccessfulRefresh == null || Duration.between(lastSuccessfulRefresh, Instant.now()).compareTo(REFRESH_INTERVAL) >= 0) {
            refresh();
        }
    }

    void refresh() {
        Map<String, SecTickerEntry> entries;
        try {
            entries = client.fetchTickerEntries();
        } catch (RuntimeException e) {
            log.warn("failed to fetch SEC ticker file; keeping the existing CIK-to-ticker mapping ({} entries) and retrying within {} minutes",
                cikToTicker.size(), RETRY_CHECK_INTERVAL_MINUTES, e);
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
        this.lastSuccessfulRefresh = Instant.now();
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
