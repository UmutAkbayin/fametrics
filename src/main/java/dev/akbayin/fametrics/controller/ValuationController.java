package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.entity.Company;
import dev.akbayin.fametrics.service.ValuationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<BigDecimal> getValuation(@Valid @RequestBody GrahamRequest request) {
        return valuationService.calculateGrahamNumber(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }
}
