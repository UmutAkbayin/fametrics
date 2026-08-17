package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.dto.DeRatioRequest;
import dev.akbayin.fametrics.dto.GrahamRequest;
import dev.akbayin.fametrics.dto.LynchFairValueRequest;
import dev.akbayin.fametrics.dto.MetricResponse;
import dev.akbayin.fametrics.dto.PbRatioRequest;
import dev.akbayin.fametrics.dto.PeTtmRequest;
import dev.akbayin.fametrics.dto.PegRatioRequest;
import dev.akbayin.fametrics.dto.PsRatioRequest;
import dev.akbayin.fametrics.dto.RoeRequest;
import dev.akbayin.fametrics.dto.SummaryRequest;
import dev.akbayin.fametrics.dto.SummaryResponse;
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
    
    @PostMapping("/pe-ttm")
    public ResponseEntity<BigDecimal> getPeTtm(@Valid @RequestBody PeTtmRequest request) {
        return valuationService.calculatePeTtm(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/pe-ttm/assessment")
    public ResponseEntity<MetricResponse> getPeTtmAssessment(@Valid @RequestBody PeTtmRequest request) {
        return valuationService.assessPeTtm(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/pb")
    public ResponseEntity<BigDecimal> getPbRatio(@Valid @RequestBody PbRatioRequest request) {
        return valuationService.calculatePbRatio(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/pb/assessment")
    public ResponseEntity<MetricResponse> getPbRatioAssessment(@Valid @RequestBody PbRatioRequest request) {
        return valuationService.assessPbRatio(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/ps")
    public ResponseEntity<BigDecimal> getPsRatio(@Valid @RequestBody PsRatioRequest request) {
        return valuationService.calculatePsRatio(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/peg")
    public ResponseEntity<BigDecimal> getPegRatio(@Valid @RequestBody PegRatioRequest request) {
        return valuationService.calculatePegRatio(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/de")
    public ResponseEntity<BigDecimal> getDeRatio(@Valid @RequestBody DeRatioRequest request) {
        return valuationService.calculateDeRatio(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/roe")
    public ResponseEntity<BigDecimal> getRoe(@Valid @RequestBody RoeRequest request) {
        return valuationService.calculateRoe(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/graham")
    public ResponseEntity<BigDecimal> getGrahamNumber(@Valid @RequestBody GrahamRequest request) {
        return valuationService.calculateGrahamNumber(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/graham/assessment")
    public ResponseEntity<MetricResponse> getGrahamNumberAssessment(@Valid @RequestBody GrahamRequest request) {
        return valuationService.assessGrahamNumber(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/lynch")
    public ResponseEntity<BigDecimal> getLynchFairValue(@Valid @RequestBody LynchFairValueRequest request) {
        return valuationService.calculateLynchFairValue(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/lynch/assessment")
    public ResponseEntity<MetricResponse> getLynchFairValueAssessment(@Valid @RequestBody LynchFairValueRequest request) {
        return valuationService.assessLynchFairValue(request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary(@Valid @RequestBody SummaryRequest request) {
        return ResponseEntity.ok(valuationService.calculateSummary(request));
    }
}
