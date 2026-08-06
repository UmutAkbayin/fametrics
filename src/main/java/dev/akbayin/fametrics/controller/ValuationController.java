package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.PbRatioRequest;
import dev.akbayin.fametrics.dto.PeTtmRequest;
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
    public ResponseEntity<BigDecimal> getGrahamNumber(@Valid @RequestBody GrahamRequest request) {
        return valuationService.calculateGrahamNumber(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }
    
    @PostMapping("/pe-ttm")
    public ResponseEntity<BigDecimal> getPeTtm(@Valid @RequestBody PeTtmRequest request) {
        return valuationService.calculatePeTtm(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/pb")
    public ResponseEntity<BigDecimal> getPbRatio(@Valid @RequestBody PbRatioRequest request) {
        return valuationService.calculatePbRatio(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }
}
