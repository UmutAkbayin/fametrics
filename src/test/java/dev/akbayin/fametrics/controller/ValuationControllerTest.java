package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.dto.DeRatioRequest;
import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.LynchFairValueRequest;
import dev.akbayin.fametrics.dto.PbRatioRequest;
import dev.akbayin.fametrics.dto.PeTtmRequest;
import dev.akbayin.fametrics.dto.PegRatioRequest;
import dev.akbayin.fametrics.dto.PsRatioRequest;
import dev.akbayin.fametrics.service.ValuationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest
@AutoConfigureRestTestClient
class ValuationControllerTest {

    @Autowired
    RestTestClient restTestClient;

    @MockitoBean
    ValuationService valuationService;

    @Test
    void getGrahamNumber_withValidRequest_shouldReturnStatus200() {
        var request = new GrahamRequest(new BigDecimal("2.93"), new BigDecimal("47.65"));

        when(valuationService.calculateGrahamNumber(any(GrahamRequest.class)))
            .thenReturn(Optional.of(new BigDecimal("56.05")));

        restTestClient.post().uri("/api/metrics/graham")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    void getGrahamNumber_withNonPositiveEps_shouldReturnStatus400() {
        var request = new GrahamRequest(BigDecimal.ZERO, new BigDecimal("47.65"));

        restTestClient.post().uri("/api/metrics/graham")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.eps").isNotEmpty();

        verifyNoInteractions(valuationService);
    }

    @Test
    void getGrahamNumber_whenServiceReturnsEmpty_shouldReturnStatus422() {
        var request = new GrahamRequest(new BigDecimal("2.93"), new BigDecimal("47.65"));

        when(valuationService.calculateGrahamNumber(any(GrahamRequest.class)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics/graham")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void getPeTtm_withValidRequest_shouldReturnStatus200() {
        var request = new PeTtmRequest(new BigDecimal("40.38"), new BigDecimal("2.93"));

        when(valuationService.calculatePeTtm(any(PeTtmRequest.class)))
            .thenReturn(Optional.of(new BigDecimal("13.78")));

        restTestClient.post().uri("/api/metrics/pe-ttm")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    void getPeTtm_withNonPositiveEps_shouldReturnStatus400() {
        var request = new PeTtmRequest(BigDecimal.ZERO, new BigDecimal("2.93"));

        restTestClient.post().uri("/api/metrics/pe-ttm")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.sharePrice").isNotEmpty();

        verifyNoInteractions(valuationService);
    }

    @Test
    void getPeTtm_whenServiceReturnsEmpty_shouldReturnStatus422() {
        var request = new PeTtmRequest(new BigDecimal("40.38"), new BigDecimal("2.93"));

        when(valuationService.calculatePeTtm(any(PeTtmRequest.class)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics/pe-ttm")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void getPbRatio_withValidRequest_shouldReturnStatus200() {
        var request = new PbRatioRequest(new BigDecimal("40.38"), new BigDecimal("2.93"));

        when(valuationService.calculatePbRatio(any(PbRatioRequest.class)))
            .thenReturn(Optional.of(new BigDecimal("13.78")));

        restTestClient.post().uri("/api/metrics/pb")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    void getPbRatio_withNonPositiveBvps_shouldReturnStatus400() {
        var request = new PbRatioRequest(BigDecimal.ZERO, new BigDecimal("2.93"));

        restTestClient.post().uri("/api/metrics/pb")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.sharePrice").isNotEmpty();

        verifyNoInteractions(valuationService);
    }

    @Test
    void getPbRatio_whenServiceReturnsEmpty_shouldReturnStatus422() {
        var request = new PbRatioRequest(new BigDecimal("40.38"), new BigDecimal("2.93"));

        when(valuationService.calculatePbRatio(any(PbRatioRequest.class)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics/pb")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void getPsRatio_withValidRequest_shouldReturnStatus200() {
        var request = new PsRatioRequest(new BigDecimal("50"), new BigDecimal("100"));

        when(valuationService.calculatePsRatio(any(PsRatioRequest.class)))
            .thenReturn(Optional.of(new BigDecimal("0.50")));

        restTestClient.post().uri("/api/metrics/ps")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    void getPsRatio_withNonPositiveMarketCap_shouldReturnStatus400() {
        var request = new PbRatioRequest(BigDecimal.ZERO, new BigDecimal("100"));

        restTestClient.post().uri("/api/metrics/ps")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.marketCap").isNotEmpty();

        verifyNoInteractions(valuationService);
    }

    @Test
    void getPsRatio_whenServiceReturnsEmpty_shouldReturnStatus422() {
        var request = new PsRatioRequest(new BigDecimal("50"), new BigDecimal("100"));

        when(valuationService.calculatePsRatio(any(PsRatioRequest.class)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics/ps")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void getPegRatio_withValidRequest_shouldReturnStatus200() {
        var request = new PegRatioRequest(new BigDecimal("40.38"), new BigDecimal("2.93"), new BigDecimal("0.15"));

        when(valuationService.calculatePegRatio(any(PegRatioRequest.class)))
            .thenReturn(Optional.of(new BigDecimal("0.92")));

        restTestClient.post().uri("/api/metrics/peg")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    void getPegRatio_withNonPositiveSharePrice_shouldReturnStatus400() {
        var request = new PegRatioRequest(BigDecimal.ZERO, new BigDecimal("2.93"), new BigDecimal("0.15"));

        restTestClient.post().uri("/api/metrics/peg")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.sharePrice").isNotEmpty();

        verifyNoInteractions(valuationService);
    }

    @Test
    void getPegRatio_whenServiceReturnsEmpty_shouldReturnStatus422() {
        var request = new PegRatioRequest(new BigDecimal("40.38"), new BigDecimal("2.93"), new BigDecimal("0.15"));

        when(valuationService.calculatePegRatio(any(PegRatioRequest.class)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics/peg")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void getDeRatio_withValidRequest_shouldReturnStatus200() {
        var request = new DeRatioRequest(new BigDecimal("150000"), new BigDecimal("100000"));

        when(valuationService.calculateDeRatio(any(DeRatioRequest.class)))
            .thenReturn(Optional.of(new BigDecimal("1.50")));

        restTestClient.post().uri("/api/metrics/de")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    void getDeRatio_withNegativeTotalLiabilities_shouldReturnStatus400() {
        var request = new DeRatioRequest(new BigDecimal("-1"), new BigDecimal("100000"));

        restTestClient.post().uri("/api/metrics/de")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.totalLiabilities").isNotEmpty();

        verifyNoInteractions(valuationService);
    }

    @Test
    void getDeRatio_whenServiceReturnsEmpty_shouldReturnStatus422() {
        var request = new DeRatioRequest(new BigDecimal("150000"), new BigDecimal("100000"));

        when(valuationService.calculateDeRatio(any(DeRatioRequest.class)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics/de")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void getLynchFairValue_withValidRequest_shouldReturnStatus200() {
        var request = new LynchFairValueRequest(new BigDecimal("2.93"), new BigDecimal("0.15"));

        when(valuationService.calculateLynchFairValue(any(LynchFairValueRequest.class)))
            .thenReturn(Optional.of(new BigDecimal("43.95")));

        restTestClient.post().uri("/api/metrics/lynch")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    void getLynchFairValue_withNonPositiveEps_shouldReturnStatus400() {
        var request = new LynchFairValueRequest(BigDecimal.ZERO, new BigDecimal("0.15"));

        restTestClient.post().uri("/api/metrics/lynch")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.eps").isNotEmpty();

        verifyNoInteractions(valuationService);
    }

    @Test
    void getLynchFairValue_whenServiceReturnsEmpty_shouldReturnStatus422() {
        var request = new LynchFairValueRequest(new BigDecimal("2.93"), new BigDecimal("0.15"));

        when(valuationService.calculateLynchFairValue(any(LynchFairValueRequest.class)))
            .thenReturn(Optional.empty());

        restTestClient.post().uri("/api/metrics/lynch")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }
}