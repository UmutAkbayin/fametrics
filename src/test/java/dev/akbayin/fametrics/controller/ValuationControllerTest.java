package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.PeTtmRequest;
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
            .thenReturn(Optional.of(new BigDecimal("56.05000")));

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
            .thenReturn(Optional.of(new BigDecimal("13.781570")));

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
}