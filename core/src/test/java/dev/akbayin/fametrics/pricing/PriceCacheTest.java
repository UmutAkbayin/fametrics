package dev.akbayin.fametrics.pricing;

import dev.akbayin.fametrics.fmp.FmpClient;
import dev.akbayin.fametrics.fmp.FmpProfile;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import dev.akbayin.fametrics.screener.ScreenerClient;
import dev.akbayin.fametrics.sec.CikTickerLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceCacheTest {

    @Mock
    private ScreenerClient screenerClient;
    @Mock
    private CikTickerLookup tickerLookup;
    @Mock
    private FmpClient fmpClient;

    @InjectMocks
    private PriceCache priceCache;

    private static ScreenerCandidate candidate(long cik) {
        return new ScreenerCandidate(
            cik, "COMPANY " + cik, LocalDate.of(2025, 12, 31), "0001-1",
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            true, OffsetDateTime.now()
        );
    }

    @Test
    void findPrice_beforeAnyRefresh_returnsEmpty() {
        assertThat(priceCache.findPrice(1L)).isEmpty();
    }

    @Test
    void refresh_happyPath_cachesPriceAndMarketCapByCik() {
        when(screenerClient.fetchCandidates(anyInt())).thenReturn(List.of(candidate(1L)));
        when(tickerLookup.findTicker(1L)).thenReturn(Optional.of("ACME"));
        when(fmpClient.fetchProfile("ACME")).thenReturn(Optional.of(
            new FmpProfile("ACME", new BigDecimal("133.66"), new BigDecimal("4016683490"))
        ));

        priceCache.refresh();

        Optional<CachedPrice> got = priceCache.findPrice(1L);
        assertThat(got).isPresent();
        assertThat(got.get().price()).isEqualByComparingTo("133.66");
        assertThat(got.get().marketCap()).isEqualByComparingTo("4016683490");
    }

    @Test
    void refresh_candidateWithNoTicker_isSkippedWithoutFailingTheRest() {
        when(screenerClient.fetchCandidates(anyInt())).thenReturn(List.of(candidate(1L), candidate(2L)));
        when(tickerLookup.findTicker(1L)).thenReturn(Optional.empty()); // e.g. a subsidiary filer with no public ticker
        when(tickerLookup.findTicker(2L)).thenReturn(Optional.of("GOOD"));
        when(fmpClient.fetchProfile("GOOD")).thenReturn(Optional.of(
            new FmpProfile("GOOD", new BigDecimal("10.00"), new BigDecimal("1000"))
        ));

        priceCache.refresh();

        assertThat(priceCache.findPrice(1L)).isEmpty();
        assertThat(priceCache.findPrice(2L)).isPresent();
    }

    @Test
    void refresh_whenFmpFailsForOneSymbol_stillCachesTheOthers() {
        when(screenerClient.fetchCandidates(anyInt())).thenReturn(List.of(candidate(1L), candidate(2L)));
        when(tickerLookup.findTicker(1L)).thenReturn(Optional.of("BAD"));
        when(tickerLookup.findTicker(2L)).thenReturn(Optional.of("GOOD"));
        when(fmpClient.fetchProfile("BAD")).thenThrow(new RuntimeException("FMP 402"));
        when(fmpClient.fetchProfile("GOOD")).thenReturn(Optional.of(
            new FmpProfile("GOOD", new BigDecimal("10.00"), new BigDecimal("1000"))
        ));

        priceCache.refresh();

        assertThat(priceCache.findPrice(1L)).isEmpty();
        assertThat(priceCache.findPrice(2L)).isPresent();
    }

    @Test
    void refresh_whenScreenerFails_keepsThePreviouslyCachedPricesInstead() {
        when(screenerClient.fetchCandidates(anyInt())).thenReturn(List.of(candidate(1L)));
        when(tickerLookup.findTicker(1L)).thenReturn(Optional.of("ACME"));
        when(fmpClient.fetchProfile("ACME")).thenReturn(Optional.of(
            new FmpProfile("ACME", new BigDecimal("50.00"), new BigDecimal("1"))
        ));
        priceCache.refresh(); // first, successful refresh

        when(screenerClient.fetchCandidates(anyInt())).thenThrow(new RuntimeException("screener unreachable"));
        priceCache.refresh(); // second refresh fails before touching the cache

        Optional<CachedPrice> stillCached = priceCache.findPrice(1L);
        assertThat(stillCached).isPresent();
        assertThat(stillCached.get().price()).isEqualByComparingTo("50.00");
    }

    @Test
    void refreshIfDue_whenScreenerSucceedsButNoTickerResolvesForAnyCandidate_keepsRetrying() {
        // The exact cascading failure observed live: CikTickerLookup's own
        // mapping was empty (SEC rate-limited it), so every candidate here
        // misses its ticker even though the screener call itself succeeds.
        // That must NOT count as "successful" — otherwise this settles into
        // the 24h cadence and stays empty long after CikTickerLookup itself
        // has already recovered.
        when(screenerClient.fetchCandidates(anyInt())).thenReturn(List.of(candidate(1L)));
        when(tickerLookup.findTicker(1L)).thenReturn(Optional.empty());

        priceCache.refreshIfDue();
        priceCache.refreshIfDue();
        priceCache.refreshIfDue();

        verify(screenerClient, times(3)).fetchCandidates(anyInt());
    }

    @Test
    void refreshIfDue_whenScreenerHasNoCandidatesAtAll_isStillTreatedAsSuccessful() {
        // Distinct from the case above: the screener genuinely has nothing
        // to give us right now (a legitimate state, not a failure), so
        // there's no benefit to retrying every 5 minutes for this reason.
        when(screenerClient.fetchCandidates(anyInt())).thenReturn(List.of());

        priceCache.refreshIfDue();
        priceCache.refreshIfDue();

        verify(screenerClient, times(1)).fetchCandidates(anyInt());
    }

    @Test
    void refreshIfDue_afterActuallyPricingSomething_doesNotRefetchImmediately() {
        when(screenerClient.fetchCandidates(anyInt())).thenReturn(List.of(candidate(1L)));
        when(tickerLookup.findTicker(1L)).thenReturn(Optional.of("ACME"));
        when(fmpClient.fetchProfile("ACME")).thenReturn(Optional.of(
            new FmpProfile("ACME", new BigDecimal("50.00"), new BigDecimal("1"))
        ));

        priceCache.refreshIfDue();
        priceCache.refreshIfDue();

        verify(screenerClient, times(1)).fetchCandidates(anyInt());
    }
}
