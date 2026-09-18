package dev.akbayin.fametrics.screener;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Talks to the screener service's {@code GET /candidates} endpoint. Called
 * fresh on every request — the screener itself has no caching/change
 * detection to defer to, and this call is cheap compared to the FMP calls
 * that follow it in the orchestration pipeline.
 */
@Component
public class ScreenerClient {

    private final RestClient restClient;

    public ScreenerClient(RestClient.Builder restClientBuilder, ScreenerProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    public List<ScreenerCandidate> fetchCandidates(int limit) {
        ScreenerCandidate[] candidates = restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/candidates").queryParam("limit", limit).build())
            .retrieve()
            .body(ScreenerCandidate[].class);

        return candidates == null ? List.of() : List.of(candidates);
    }
}
