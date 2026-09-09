package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.domain.Assessment;
import dev.akbayin.fametrics.domain.Benchmark;
import dev.akbayin.fametrics.domain.Metric;
import dev.akbayin.fametrics.domain.Rating;
import dev.akbayin.fametrics.dto.CapitalStructure;
import dev.akbayin.fametrics.dto.FundamentalData;
import dev.akbayin.fametrics.dto.MarketData;
import dev.akbayin.fametrics.dto.MetricResponse;
import dev.akbayin.fametrics.dto.SummaryRequest;
import dev.akbayin.fametrics.dto.SummaryResponse;
import dev.akbayin.fametrics.service.ValuationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest
@AutoConfigureRestTestClient
class ValuationControllerTest {

    private static final BigDecimal VALUE = new BigDecimal("13.78");

    private static final Map<Metric, String> VALUE_PATHS = Map.ofEntries(
        Map.entry(Metric.PE_TTM, "/pe-ttm"),
        Map.entry(Metric.PB_RATIO, "/pb"),
        Map.entry(Metric.PS_RATIO, "/ps"),
        Map.entry(Metric.PEG_RATIO, "/peg"),
        Map.entry(Metric.DE_RATIO, "/de"),
        Map.entry(Metric.ROE, "/roe"),
        Map.entry(Metric.GRAHAM_NUMBER, "/graham"),
        Map.entry(Metric.LYNCH_FAIR_VALUE, "/lynch")
    );

    @Autowired
    RestTestClient restTestClient;

    @MockitoBean
    ValuationService valuationService;

    private static SummaryRequest validRequest() {
        return new SummaryRequest(
            new MarketData(new BigDecimal("40.38"), new BigDecimal("2.93"), new BigDecimal("47.65")),
            new FundamentalData(new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("0.15")),
            new CapitalStructure(new BigDecimal("150000"), new BigDecimal("100000"), new BigDecimal("15000"))
        );
    }

    private static MetricResponse metricResponse(Metric metric) {
        return new MetricResponse(
            metric,
            VALUE,
            new Assessment(Rating.FAVORABLE, "Low"),
            new Benchmark(new BigDecimal("1"), new BigDecimal("2"), "explanation"),
            "description",
            "interpretation"
        );
    }

    @ParameterizedTest
    @EnumSource(Metric.class)
    void getValue_whenServiceReturnsValue_shouldReturnStatus200(Metric metric) {
        when(valuationService.calculate(any(SummaryRequest.class), eq(metric)))
            .thenReturn(Optional.of(VALUE));

        restTestClient.post().uri("/api/metrics" + VALUE_PATHS.get(metric))
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequest())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(BigDecimal.class)
            .isEqualTo(VALUE);
    }

    @ParameterizedTest
    @EnumSource(Metric.class)
    void getValue_whenServiceReturnsEmpty_shouldReturnStatus422(Metric metric) {
        when(valuationService.calculate(any(SummaryRequest.class), eq(metric)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics" + VALUE_PATHS.get(metric))
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequest())
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @ParameterizedTest
    @EnumSource(Metric.class)
    void getAssessment_whenServiceReturnsValue_shouldReturnStatus200(Metric metric) {
        when(valuationService.assess(any(SummaryRequest.class), eq(metric)))
            .thenReturn(Optional.of(metricResponse(metric)));

        restTestClient.post().uri("/api/metrics" + VALUE_PATHS.get(metric) + "/assessment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequest())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.metric").isEqualTo(metric.name())
            .jsonPath("$.value").isEqualTo(13.78);
    }

    @ParameterizedTest
    @EnumSource(Metric.class)
    void getAssessment_whenServiceReturnsEmpty_shouldReturnStatus422(Metric metric) {
        when(valuationService.assess(any(SummaryRequest.class), eq(metric)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics" + VALUE_PATHS.get(metric) + "/assessment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequest())
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void getSummaryAssessment_shouldReturnStatus200WithAllMetrics() {
        var metrics = List.of(metricResponse(Metric.PE_TTM), metricResponse(Metric.GRAHAM_NUMBER));

        when(valuationService.assessSummary(any(SummaryRequest.class)))
            .thenReturn(new SummaryResponse(metrics));

        restTestClient.post().uri("/api/metrics/summary/assessment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequest())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.metrics.length()").isEqualTo(2)
            .jsonPath("$.metrics[0].metric").isEqualTo(Metric.PE_TTM.name())
            .jsonPath("$.metrics[1].metric").isEqualTo(Metric.GRAHAM_NUMBER.name());
    }

    @Test
    void getSummaryAssessment_whenServiceReturnsNoMetrics_shouldReturnStatus200WithEmptyList() {
        when(valuationService.assessSummary(any(SummaryRequest.class)))
            .thenReturn(new SummaryResponse(List.of()));

        restTestClient.post().uri("/api/metrics/summary/assessment")
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequest())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.metrics.length()").isEqualTo(0);
    }
}
