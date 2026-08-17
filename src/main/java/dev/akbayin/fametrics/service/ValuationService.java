package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.domain.GrahamNumberAssessor;
import dev.akbayin.fametrics.domain.LynchFairValueAssessor;
import dev.akbayin.fametrics.domain.PbRatioAssessor;
import dev.akbayin.fametrics.dto.DeRatioRequest;
import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.LynchFairValueRequest;
import dev.akbayin.fametrics.dto.MetricResponse;
import dev.akbayin.fametrics.domain.PeTtmAssessor;
import dev.akbayin.fametrics.dto.PbRatioRequest;
import dev.akbayin.fametrics.dto.PeTtmRequest;
import dev.akbayin.fametrics.dto.PegRatioRequest;
import dev.akbayin.fametrics.dto.PsRatioRequest;
import dev.akbayin.fametrics.dto.RoeRequest;
import dev.akbayin.fametrics.dto.SummaryRequest;
import dev.akbayin.fametrics.dto.SummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ValuationService {

    private final GrahamNumberAssessor grahamNumberAssessor;
    private final LynchFairValueAssessor lynchFairValueAssessor;
    private final PeTtmAssessor peTtmAssessor;
    private final PbRatioAssessor pbRatioAssessor;

    private static final BigDecimal MULTIPLIER = new BigDecimal("22.5");
    private static final int SCALE = 2;

    public SummaryResponse calculateSummary(SummaryRequest request) {
        var peTtm = calculatePeTtm(new PeTtmRequest(request.sharePrice(), request.eps())).orElse(null);
        var pbRatio = calculatePbRatio(new PbRatioRequest(request.sharePrice(), request.bvps())).orElse(null);
        var psRatio = calculatePsRatio(new PsRatioRequest(request.marketCap(), request.totalRevenue()))
            .orElse(null);
        var pegRatio = calculatePegRatio(new PegRatioRequest(request.sharePrice(), request.eps(), request.epsGrowthRate()))
            .orElse(null);
        var deRatio = calculateDeRatio(new DeRatioRequest(request.totalLiabilities(), request.totalEquity()))
            .orElse(null);
        var roeRatio = calculateRoe(new RoeRequest(request.netIncome(), request.totalEquity())).orElse(null);
        var grahamNumber = calculateGrahamNumber(new GrahamRequest(request.eps(), request.bvps(), request.sharePrice()))
            .orElse(null);
        var lynchFairValue = calculateLynchFairValue(
            new LynchFairValueRequest(request.eps(), request.epsGrowthRate(), request.sharePrice()))
            .orElse(null);

        return new SummaryResponse(
            peTtm,
            pbRatio,
            psRatio,
            pegRatio,
            deRatio,
            roeRatio,
            grahamNumber,
            lynchFairValue
        );
    }

    public Optional<MetricResponse> assessPeTtm(PeTtmRequest request) {
        return calculatePeTtm(request)
            .map(value -> {
                var evaluation = peTtmAssessor.evaluate(request, value);

                return new MetricResponse(
                    peTtmAssessor.metric(),
                    value,
                    evaluation.assessment(),
                    evaluation.benchmark(),
                    peTtmAssessor.description(),
                    peTtmAssessor.interpretation(request, value, evaluation.assessment())
                );
            });
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

    public Optional<MetricResponse> assessPbRatio(PbRatioRequest request) {
        return calculatePbRatio(request)
            .map(value -> {
                var evaluation = pbRatioAssessor.evaluate(request, value);

                return new MetricResponse(
                    pbRatioAssessor.metric(),
                    value,
                    evaluation.assessment(),
                    evaluation.benchmark(),
                    pbRatioAssessor.description(),
                    pbRatioAssessor.interpretation(request, value, evaluation.assessment())
                );
            });
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
        var epsGrowthRate = request.epsGrowthRate();

        if (epsGrowthRate == null || epsGrowthRate.signum() <= 0) {
            return Optional.empty();
        }

        var peRatio = calculatePeTtm(new PeTtmRequest(request.sharePrice(), request.eps()));

        return peRatio.map(pe ->
            pe.divide(epsGrowthRate.multiply(new BigDecimal("100")), SCALE, RoundingMode.HALF_UP));
    }

    public Optional<BigDecimal> calculateDeRatio(DeRatioRequest request) {
        var totalLiabilities = request.totalLiabilities();
        var totalEquity = request.totalEquity();

        if (totalLiabilities == null || totalLiabilities.signum() < 0
            || totalEquity == null || totalEquity.signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal result = totalLiabilities.divide(totalEquity, SCALE, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    public Optional<BigDecimal> calculateRoe(RoeRequest request) {
        var netIncome = request.netIncome();
        var totalEquity = request.totalEquity();

        if (netIncome == null || totalEquity == null || totalEquity.signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal result = netIncome.divide(totalEquity, SCALE, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    public Optional<MetricResponse> assessGrahamNumber(GrahamRequest request) {
        return calculateGrahamNumber(request)
            .map(value -> {
                var evaluation = grahamNumberAssessor.evaluate(request, value);

                return new MetricResponse(
                    grahamNumberAssessor.metric(),
                    value,
                    evaluation.assessment(),
                    evaluation.benchmark(),
                    grahamNumberAssessor.description(),
                    grahamNumberAssessor.interpretation(request, value, evaluation.assessment())
                );
            });
    }

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

    public Optional<MetricResponse> assessLynchFairValue(LynchFairValueRequest request) {
        return calculateLynchFairValue(request)
            .map(value -> {
                var evaluation = lynchFairValueAssessor.evaluate(request, value);

                return new MetricResponse(
                    lynchFairValueAssessor.metric(),
                    value,
                    evaluation.assessment(),
                    evaluation.benchmark(),
                    lynchFairValueAssessor.description(),
                    lynchFairValueAssessor.interpretation(request, value, evaluation.assessment())
                );
            });
    }

    public Optional<BigDecimal> calculateLynchFairValue(LynchFairValueRequest request) {
        var eps = request.eps();
        var epsGrowthRate = request.epsGrowthRate();

        if (eps == null || eps.signum() <= 0 || epsGrowthRate == null) {
            return Optional.empty();
        }

        BigDecimal result = eps
            .multiply(epsGrowthRate.multiply(new BigDecimal("100")))
            .setScale(SCALE, RoundingMode.HALF_UP);

        return Optional.of(result);
    }

    private static boolean anyNonPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value == null || value.signum() <= 0) {
                return true;
            }
        }
        return false;
    }
}
