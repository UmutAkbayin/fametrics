package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.entity.Company;
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
    @MethodSource("provideInvalidCompanies")
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

        assertThat(result).contains(new BigDecimal("8.407630"));
    }

    private static Stream<Arguments> provideInvalidCompanies() {
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