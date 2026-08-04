package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.entity.Company;
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

    public Optional<BigDecimal> calculateGrahamNumber(GrahamRequest request) {
        if (request.eps() == null ||
            request.bvps() == null ||
            request.eps().compareTo(BigDecimal.ZERO) <= 0 ||
            request.bvps().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal product = MULTIPLIER
            .multiply(request.eps())
            .multiply(request.bvps());

        return Optional.of(product.sqrt(new MathContext(6, RoundingMode.HALF_UP)));
    }


}
