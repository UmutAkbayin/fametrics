package dev.akbayin.fametrics.sec;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Fetches SEC's free CIK-to-ticker mapping file. Unlike the screener or FMP,
 * this URL is fixed (SEC's own well-known file), not something we'd ever
 * point elsewhere, so it isn't part of {@link SecProperties}.
 */
@Component
public class SecTickerClient {

    private static final String TICKERS_URL = "https://www.sec.gov/files/company_tickers.json";

    private final RestClient restClient;
    private final SecProperties properties;

    public SecTickerClient(RestClient.Builder restClientBuilder, SecProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    /**
     * Returns the raw file contents, keyed by SEC's own stringified index
     * ("0", "1", ...) — that key carries no meaning of its own, callers
     * should only use the entries' values.
     */
    public Map<String, SecTickerEntry> fetchTickerEntries() {
        return restClient.get()
            .uri(TICKERS_URL)
            // SEC requires a contact address in the User-Agent on every
            // request or it rejects them outright — same requirement the
            // screener's own SEC calls have.
            .header(HttpHeaders.USER_AGENT, "Fametrics-Core/1.0 (%s)".formatted(properties.userAgentEmail()))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
    }
}
