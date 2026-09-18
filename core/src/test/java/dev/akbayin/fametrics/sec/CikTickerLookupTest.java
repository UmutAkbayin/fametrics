package dev.akbayin.fametrics.sec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CikTickerLookupTest {

    @Mock
    private SecTickerClient client;

    @InjectMocks
    private CikTickerLookup lookup;

    @Test
    void findTicker_beforeAnyRefresh_returnsEmpty() {
        assertThat(lookup.findTicker(320193L)).isEmpty();
    }

    @Test
    void refresh_thenFindTicker_returnsTheTickerForAKnownCik() {
        when(client.fetchTickerEntries()).thenReturn(Map.of(
            "0", new SecTickerEntry(320193L, "AAPL", "Apple Inc.")
        ));

        lookup.refresh();

        assertThat(lookup.findTicker(320193L)).contains("AAPL");
    }

    @Test
    void findTicker_whenCikUnknown_returnsEmpty() {
        when(client.fetchTickerEntries()).thenReturn(Map.of());

        lookup.refresh();

        assertThat(lookup.findTicker(999L)).isEmpty();
    }

    @Test
    void refresh_whenCikHasMultipleEntries_keepsTheFirstOne() {
        // LinkedHashMap to pin iteration order deterministically — mirrors
        // SEC's real file, which lists the primary/common-class ticker
        // (GOOGL) before a second share class (GOOG) for the same CIK.
        Map<String, SecTickerEntry> entries = new LinkedHashMap<>();
        entries.put("0", new SecTickerEntry(1652044L, "GOOGL", "Alphabet Inc."));
        entries.put("1", new SecTickerEntry(1652044L, "GOOG", "Alphabet Inc."));
        when(client.fetchTickerEntries()).thenReturn(entries);

        lookup.refresh();

        assertThat(lookup.findTicker(1652044L)).contains("GOOGL");
    }
}
