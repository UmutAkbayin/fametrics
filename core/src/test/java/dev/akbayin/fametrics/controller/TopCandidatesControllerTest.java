package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.ranking.AssessedCandidate;
import dev.akbayin.fametrics.ranking.FinalCandidate;
import dev.akbayin.fametrics.ranking.RankedCandidate;
import dev.akbayin.fametrics.ranking.TopCandidatesService;
import dev.akbayin.fametrics.ranking.ValueScore;
import dev.akbayin.fametrics.dto.SummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static dev.akbayin.fametrics.screener.ScreenerCandidateTestBuilder.screenerCandidate;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(TopCandidatesController.class)
@AutoConfigureRestTestClient
class TopCandidatesControllerTest {

    @Autowired
    RestTestClient restTestClient;

    @MockitoBean
    TopCandidatesService topCandidatesService;

    private static FinalCandidate finalCandidate(long cik, String name) {
        var candidate = screenerCandidate().cik(cik).name(name).qualityScore(new BigDecimal("80")).build();
        var price = new CachedPrice("TICK", new BigDecimal("50"), new BigDecimal("1000"), Instant.now());
        var assessed = new AssessedCandidate(candidate, price, new SummaryResponse(List.of()));
        var valueScore = new ValueScore(null, null, null, null, null, new BigDecimal("60"));
        return new FinalCandidate(new RankedCandidate(assessed, valueScore), new BigDecimal("70.00"));
    }

    @Test
    void getTopCandidates_returnsTheServiceResultAsJson() {
        when(topCandidatesService.getTopCandidates(20)).thenReturn(List.of(finalCandidate(1L, "ACME INC")));

        restTestClient.get().uri("/api/candidates/top")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].cik").isEqualTo(1)
            .jsonPath("$[0].name").isEqualTo("ACME INC")
            .jsonPath("$[0].finalScore").isEqualTo(70.00);
    }

    @Test
    void getTopCandidates_defaultsLimitToTwenty() {
        when(topCandidatesService.getTopCandidates(anyInt())).thenReturn(List.of());

        restTestClient.get().uri("/api/candidates/top")
            .exchange()
            .expectStatus().isOk();

        verify(topCandidatesService).getTopCandidates(20);
    }

    @Test
    void getTopCandidates_passesExplicitLimitThrough() {
        when(topCandidatesService.getTopCandidates(anyInt())).thenReturn(List.of());

        restTestClient.get().uri("/api/candidates/top?limit=5")
            .exchange()
            .expectStatus().isOk();

        verify(topCandidatesService).getTopCandidates(5);
    }

    @Test
    void getTopCandidates_whenScreenerIsUnreachable_returns503NotARaw500() {
        when(topCandidatesService.getTopCandidates(anyInt()))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("connection refused"));

        restTestClient.get().uri("/api/candidates/top")
            .exchange()
            .expectStatus().isEqualTo(503)
            .expectBody()
            .jsonPath("$.error").exists();
    }
}
