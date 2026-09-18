package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.domain.PsRatioAssessor;
import dev.akbayin.fametrics.domain.RoeAssessor;
import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.pricing.PriceCache;
import dev.akbayin.fametrics.screener.ScreenerCandidate;
import dev.akbayin.fametrics.screener.ScreenerClient;
import dev.akbayin.fametrics.service.ValuationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopCandidatesServiceTest {

    @Mock
    private ScreenerClient screenerClient;
    @Mock
    private PriceCache priceCache;

    private final CandidateAssessor candidateAssessor = new CandidateAssessor(new ValuationService(List.of(new RoeAssessor(), new PsRatioAssessor())));
    private final ValueScoreCalculator valueScoreCalculator = new ValueScoreCalculator();
    private final FinalScoreCalculator finalScoreCalculator = new FinalScoreCalculator();

    private TopCandidatesService service() {
        return new TopCandidatesService(screenerClient, priceCache, candidateAssessor, valueScoreCalculator, finalScoreCalculator);
    }

    private static ScreenerCandidate candidate(long cik, BigDecimal qualityScore) {
        return new ScreenerCandidate(
            cik, "COMPANY " + cik, LocalDate.of(2025, 12, 31), "0001-1",
            null, new BigDecimal("500"), new BigDecimal("1000"), new BigDecimal("150"),
            null, null, null, new BigDecimal("2000"), null, null, null, null, null, null,
            null, null, null, qualityScore,
            true, OffsetDateTime.now()
        );
    }

    @Test
    void getTopCandidates_skipsCandidatesWithNoCachedPrice() {
        var withPrice = candidate(1L, new BigDecimal("80"));
        var withoutPrice = candidate(2L, new BigDecimal("90"));
        when(screenerClient.fetchCandidates(200)).thenReturn(List.of(withPrice, withoutPrice));
        when(priceCache.findPrice(1L)).thenReturn(Optional.of(new CachedPrice("ONE", new BigDecimal("50"), new BigDecimal("1000"), Instant.now())));
        when(priceCache.findPrice(2L)).thenReturn(Optional.empty());

        List<FinalCandidate> top = service().getTopCandidates(20);

        assertThat(top).hasSize(1);
        assertThat(top.get(0).ranked().assessed().candidate().cik()).isEqualTo(1L);
    }

    @Test
    void getTopCandidates_respectsLimit() {
        var a = candidate(1L, new BigDecimal("80"));
        var b = candidate(2L, new BigDecimal("70"));
        when(screenerClient.fetchCandidates(200)).thenReturn(List.of(a, b));
        when(priceCache.findPrice(1L)).thenReturn(Optional.of(new CachedPrice("A", new BigDecimal("50"), new BigDecimal("1000"), Instant.now())));
        when(priceCache.findPrice(2L)).thenReturn(Optional.of(new CachedPrice("B", new BigDecimal("50"), new BigDecimal("1000"), Instant.now())));

        List<FinalCandidate> top = service().getTopCandidates(1);

        assertThat(top).hasSize(1);
    }

    @Test
    void getTopCandidates_noCandidatesHavePrices_returnsEmptyList() {
        when(screenerClient.fetchCandidates(200)).thenReturn(List.of(candidate(1L, new BigDecimal("80"))));
        when(priceCache.findPrice(1L)).thenReturn(Optional.empty());

        List<FinalCandidate> top = service().getTopCandidates(20);

        assertThat(top).isEmpty();
    }
}
