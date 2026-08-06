package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.LynchFairValueRequest;
import dev.akbayin.fametrics.dto.PbRatioRequest;
import dev.akbayin.fametrics.dto.PeTtmRequest;
import dev.akbayin.fametrics.dto.PegRatioRequest;
import dev.akbayin.fametrics.dto.PsRatioRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ValuationService {

    private static final BigDecimal MULTIPLIER = new BigDecimal("22.5");
    private static final int SCALE = 2;

    public Optional<BigDecimal> calculateGrahamNumber(GrahamRequest request) {
        var eps = request.eps();
        var bvps = request.bvps();

        if (anyNonPositive(eps, bvps)) {
            return Optional.empty();
        }

        BigDecimal product = MULTIPLIER
            .multiply(eps)
            .multiply(bvps);

        BigDecimal result = product
            .sqrt(MathContext.DECIMAL64)
            .setScale(SCALE, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    public Optional<BigDecimal> calculatePeTtm(PeTtmRequest request) {
        var sharePrice = request.sharePrice();
        var eps = request.eps();

        if (anyNonPositive(sharePrice, eps)) {
            return Optional.empty();
        }

        BigDecimal result = sharePrice.divide(eps, SCALE, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    public Optional<BigDecimal> calculatePbRatio(PbRatioRequest request) {
        var sharePrice = request.sharePrice();
        var bvps = request.bvps();

        if (anyNonPositive(sharePrice, bvps)) {
            return Optional.empty();
        }

        BigDecimal result = sharePrice.divide(bvps, SCALE, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    public Optional<BigDecimal> calculatePsRatio(PsRatioRequest request) {
        var marketCap = request.marketCap();
        var totalRevenue = request.totalRevenue();

        if (anyNonPositive(marketCap, totalRevenue)) {
            return Optional.empty();
        }

        BigDecimal result = marketCap.divide(totalRevenue, SCALE, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    public Optional<BigDecimal> calculatePegRatio(PegRatioRequest request) {
        var peRatio = calculatePeTtm(new PeTtmRequest(request.sharePrice(), request.eps()));
        var epsGrowthRate = request.epsGrowthRate();

        return peRatio.map(pe ->
            pe.divide(epsGrowthRate.multiply(new BigDecimal("100")), SCALE, RoundingMode.HALF_UP));
    }

    public Optional<BigDecimal> calculateLynchFairValue(LynchFairValueRequest request) {
        var eps = request.eps();
        var epsGrowthRate = request.epsGrowthRate();

        if (eps == null || eps.compareTo(BigDecimal.ZERO) <= 0 || epsGrowthRate == null) {
            return Optional.empty();
        }

        BigDecimal result = eps
            .multiply(epsGrowthRate.multiply(new BigDecimal("100")))
            .setScale(SCALE, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    private static boolean anyNonPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
                return true;
            }
        }
        return false;
    }
}
