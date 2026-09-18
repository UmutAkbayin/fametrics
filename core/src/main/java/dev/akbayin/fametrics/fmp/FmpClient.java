package dev.akbayin.fametrics.fmp;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Talks to FMP's {@code /stable/profile} endpoint. Deliberately not
 * {@code /stable/quote}: verified empirically (real API key, real
 * candidates from our own pool) that {@code /quote}/{@code /quote-short}
 * are free-tier-gated to a small large-cap allow-list — AZZ, Hawkins,
 * PetMed Express, Logitech all returned the identical "premium" error as a
 * made-up symbol. {@code /stable/profile} is not allow-list-gated and
 * additionally returns marketCap, so it covers both price and market cap
 * in one call.
 */
@Component
public class FmpClient {

    private static final String BASE_URL = "https://financialmodelingprep.com";

    private final RestClient restClient;
    private final FmpProperties properties;

    public FmpClient(RestClient.Builder restClientBuilder, FmpProperties properties) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.properties = properties;
    }

    /**
     * The profile for symbol, or empty if FMP has nothing for it — covers
     * both "no such symbol" (an empty JSON array) and "symbol not permitted
     * on this plan" (a non-JSON error body), neither of which should be
     * treated as fatal by a caller refreshing many symbols in a batch.
     */
    public Optional<FmpProfile> fetchProfile(String symbol) {
        FmpProfile[] profiles = restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/stable/profile")
                .queryParam("symbol", symbol)
                .queryParam("apikey", properties.apiKey())
                .build())
            .retrieve()
            .body(FmpProfile[].class);

        return profiles == null || profiles.length == 0 ? Optional.empty() : Optional.of(profiles[0]);
    }
}
