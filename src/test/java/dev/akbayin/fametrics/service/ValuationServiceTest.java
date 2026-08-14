package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.dto.DeRatioRequest;
import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.LynchFairValueRequest;
import dev.akbayin.fametrics.dto.PbRatioRequest;
import dev.akbayin.fametrics.dto.PeTtmRequest;
import dev.akbayin.fametrics.dto.PegRatioRequest;
import dev.akbayin.fametrics.dto.PsRatioRequest;
import dev.akbayin.fametrics.dto.RoeRequest;
import dev.akbayin.fametrics.dto.SummaryRequest;
import dev.akbayin.fametrics.dto.SummaryResponse;
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
        var result = valuationService.calculateGrahamNumber(new GrahamRequest(eps, bvps, null));

        assertThat(result).isEmpty();
    }

    @Test
    void calculateGrahamNumber_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculateGrahamNumber(new GrahamRequest(
            new BigDecimal("0.89"),
            new BigDecimal("3.53"),
            null
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

    @ParameterizedTest
    @MethodSource("provideInvalidBigDecimalPairs")
    void calculatePegRatio_whenInputIsInvalid_shouldReturnEmpty(BigDecimal sharePrice, BigDecimal eps) {
        var result = valuationService.calculatePegRatio(new PegRatioRequest(sharePrice, eps, new BigDecimal("0.15")));

        assertThat(result).isEmpty();
    }

    @Test
    void calculatePegRatio_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculatePegRatio(new PegRatioRequest(
            new BigDecimal("40.38"),
            new BigDecimal("2.93"),
            new BigDecimal("0.15")
        ));

        assertThat(result).contains(new BigDecimal("0.92"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDeRatioInputs")
    void calculateDeRatio_whenInputIsInvalid_shouldReturnEmpty(BigDecimal totalLiabilities, BigDecimal totalEquity) {
        var result = valuationService.calculateDeRatio(new DeRatioRequest(totalLiabilities, totalEquity));

        assertThat(result).isEmpty();
    }

    @Test
    void calculateDeRatio_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculateDeRatio(new DeRatioRequest(
            new BigDecimal("150000"),
            new BigDecimal("100000")
        ));

        assertThat(result).contains(new BigDecimal("1.50"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidRoeInputs")
    void calculateRoe_whenInputIsInvalid_shouldReturnEmpty(BigDecimal netIncome, BigDecimal totalEquity) {
        var result = valuationService.calculateRoe(new RoeRequest(netIncome, totalEquity));

        assertThat(result).isEmpty();
    }

    @Test
    void calculateRoe_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculateRoe(new RoeRequest(
            new BigDecimal("15000"),
            new BigDecimal("100000")
        ));

        assertThat(result).contains(new BigDecimal("0.15"));
    }

    @Test
    void calculateRoe_whenNetIncomeIsNegative_shouldReturnNegativeValue() {
        var result = valuationService.calculateRoe(new RoeRequest(
            new BigDecimal("-15000"),
            new BigDecimal("100000")
        ));

        assertThat(result).contains(new BigDecimal("-0.15"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidLynchFairValueInputs")
    void calculateLynchFairValue_whenInputIsInvalid_shouldReturnEmpty(BigDecimal eps, BigDecimal epsGrowthRate) {
        var result = valuationService.calculateLynchFairValue(new LynchFairValueRequest(eps, epsGrowthRate, null));

        assertThat(result).isEmpty();
    }

    @Test
    void calculateLynchFairValue_whenInputIsValid_shouldReturnValue() {
        var result = valuationService.calculateLynchFairValue(new LynchFairValueRequest(
            new BigDecimal("2.93"),
            new BigDecimal("0.15"),
            null
        ));

        assertThat(result).contains(new BigDecimal("43.95"));
    }

    @Test
    void calculateSummary_whenAllInputsAreValid_shouldReturnFullyPopulatedResult() {
        var result = valuationService.calculateSummary(new SummaryRequest(
            new BigDecimal("40.38"),
            new BigDecimal("2.93"),
            new BigDecimal("2.93"),
            new BigDecimal("50"),
            new BigDecimal("100"),
            new BigDecimal("0.15"),
            new BigDecimal("150000"),
            new BigDecimal("100000"),
            new BigDecimal("15000")
        ));

        assertThat(result).isEqualTo(new SummaryResponse(
            new BigDecimal("13.78"),
            new BigDecimal("13.78"),
            new BigDecimal("0.50"),
            new BigDecimal("0.92"),
            new BigDecimal("1.50"),
            new BigDecimal("0.15"),
            new BigDecimal("13.90"),
            new BigDecimal("43.95")
        ));
    }

    @Test
    void calculateSummary_whenSomeInputsAreInvalid_shouldReturnPartialResultWithNullFields() {
        var result = valuationService.calculateSummary(new SummaryRequest(
            new BigDecimal("40.38"),
            new BigDecimal("2.93"),
            new BigDecimal("2.93"),
            null,
            new BigDecimal("100"),
            null,
            null,
            new BigDecimal("100000"),
            new BigDecimal("15000")
        ));

        assertThat(result).isEqualTo(new SummaryResponse(
            new BigDecimal("13.78"),
            new BigDecimal("13.78"),
            null,
            null,
            null,
            new BigDecimal("0.15"),
            new BigDecimal("13.90"),
            null
        ));
    }

    @Test
    void calculateSummary_whenAllInputsAreInvalid_shouldReturnAllNullFields() {
        var result = valuationService.calculateSummary(new SummaryRequest(
            null, null, null, null, null, null, null, null, null
        ));

        assertThat(result).isEqualTo(new SummaryResponse(
            null, null, null, null, null, null, null, null
        ));
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

    private static Stream<Arguments> provideInvalidDeRatioInputs() {
        return Stream.of(
            Arguments.of(null, new BigDecimal("10.0")),
            Arguments.of(new BigDecimal("2.5"), null),

            Arguments.of(new BigDecimal("2.5"), BigDecimal.ZERO),

            Arguments.of(new BigDecimal("-1.5"), new BigDecimal("10.0")),
            Arguments.of(new BigDecimal("2.5"), new BigDecimal("-5.0"))
        );
    }

    private static Stream<Arguments> provideInvalidRoeInputs() {
        return Stream.of(
            Arguments.of(null, new BigDecimal("10.0")),
            Arguments.of(new BigDecimal("2.5"), null),

            Arguments.of(new BigDecimal("2.5"), BigDecimal.ZERO),
            Arguments.of(new BigDecimal("2.5"), new BigDecimal("-5.0"))
        );
    }

    private static Stream<Arguments> provideInvalidLynchFairValueInputs() {
        return Stream.of(
            Arguments.of(null, new BigDecimal("0.15")),
            Arguments.of(BigDecimal.ZERO, new BigDecimal("0.15")),
            Arguments.of(new BigDecimal("-2.93"), new BigDecimal("0.15")),
            Arguments.of(new BigDecimal("2.93"), null)
        );
    }
}