package dev.akbayin.fametrics.service;

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

    public Optional<BigDecimal> calculateGrahamNumber(Company company) {
        if (company.getEps() == null ||
            company.getBvps() == null ||
            company.getEps().compareTo(BigDecimal.ZERO) <= 0 ||
            company.getBvps().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal product = MULTIPLIER
            .multiply(company.getEps())
            .multiply(company.getBvps());

        return Optional.of(product.sqrt(new MathContext(6, RoundingMode.HALF_UP)));
    }
}
