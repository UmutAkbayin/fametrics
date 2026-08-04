package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.entity.Company;
import dev.akbayin.fametrics.service.ValuationService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@AllArgsConstructor
@RestController
@RequestMapping("/api/metrics")
public class ValuationController {

    private final ValuationService valuationService;

    @PostMapping("/graham")
    public ResponseEntity<BigDecimal> getValuation(@RequestBody Company company) {
        return valuationService.calculateGrahamNumber(company)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }
}
