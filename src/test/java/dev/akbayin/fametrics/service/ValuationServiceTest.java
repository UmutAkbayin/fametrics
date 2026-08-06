package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.PbRatioRequest;
import dev.akbayin.fametrics.dto.PeTtmRequest;
import dev.akbayin.fametrics.dto.PsRatioRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ValuationServiceTest {

    @InjectMocks
    ValuationService valuationService;

    @ParameterizedTest
    @MethodSource("provideInvalidBigDecimalPairs")
    void calculateGrahamNumber_whenInputIsInvalid_shouldReturnEmpty(BigDecimal eps, BigDecimal bvps) {
        var result = valuationService.calculateGrahamNumber(new GrahamRequest(eps, bvps));

        assertThat(result).isEmpty();
    }

    @Test
    void calculateGrahamNumber_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculateGrahamNumber(new GrahamRequest(
            new BigDecimal("0.89"),
            new BigDecimal("3.53")
        ));

        assertThat(result).contains(new BigDecimal("8.41"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidBigDecimalPairs")
    void calculatePeTtm_whenInputIsInvalid_shouldReturnEmpty(BigDecimal sharePrice, BigDecimal eps) {
        var result = valuationService.calculatePeTtm(new PeTtmRequest(sharePrice, eps));

        assertThat(result).isEmpty();
    }

    @Test
    void calculatePeTtm_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculatePeTtm(new PeTtmRequest(
            new BigDecimal("40.38"),
            new BigDecimal("2.93")
        ));

        assertThat(result).contains(new BigDecimal("13.78"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidBigDecimalPairs")
    void calculatePbRatio_whenInputIsInvalid_shouldReturnEmpty(BigDecimal sharePrice, BigDecimal bvps) {
        var result = valuationService.calculatePbRatio(new PbRatioRequest(sharePrice, bvps));

        assertThat(result).isEmpty();
    }

    @Test
    void calculatePbRatio_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculatePbRatio(new PbRatioRequest(
            new BigDecimal("40.38"),
            new BigDecimal("2.93")
        ));

        assertThat(result).contains(new BigDecimal("13.78"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidBigDecimalPairs")
    void calculatePsRatio_whenInputIsInvalid_shouldReturnEmpty(BigDecimal marketCap, BigDecimal totalRevenue) {
        var result = valuationService.calculatePsRatio(new PsRatioRequest(marketCap, totalRevenue));

        assertThat(result).isEmpty();
    }

    @Test
    void calculatePsRatio_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculatePsRatio(new PsRatioRequest(
            new BigDecimal("50"),
            new BigDecimal("100")
        ));

        assertThat(result).contains(new BigDecimal("0.50"));
    }

    private static Stream<Arguments> provideInvalidBigDecimalPairs() {
        return Stream.of(
            Arguments.of(null, new BigDecimal("10.0")),
            Arguments.of(new BigDecimal("2.5"), null),

            Arguments.of(BigDecimal.ZERO, new BigDecimal("10.0")),
            Arguments.of(new BigDecimal("2.5"), BigDecimal.ZERO),

            Arguments.of(new BigDecimal("-1.5"), new BigDecimal("10.0")),
            Arguments.of(new BigDecimal("2.5"), new BigDecimal("-5.0"))
        );
    }
}