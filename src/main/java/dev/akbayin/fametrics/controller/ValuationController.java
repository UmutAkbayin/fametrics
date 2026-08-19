package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.domain.Metric;
import dev.akbayin.fametrics.dto.MetricResponse;
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
    public ResponseEntity<BigDecimal> getPeTtm(@Valid @RequestBody SummaryRequest request) {
        return valuationService.calculate(request, Metric.PE_TTM)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/pe-ttm/assessment")
    public ResponseEntity<MetricResponse> getPeTtmAssessment(@Valid @RequestBody SummaryRequest request) {
        return valuationService.assess(request, Metric.PE_TTM)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/pb")
    public ResponseEntity<BigDecimal> getPbRatio(@Valid @RequestBody SummaryRequest request) {
        return valuationService.calculate(request, Metric.PB_RATIO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/pb/assessment")
    public ResponseEntity<MetricResponse> getPbRatioAssessment(@Valid @RequestBody SummaryRequest request) {
        return valuationService.assess(request, Metric.PB_RATIO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/ps")
    public ResponseEntity<BigDecimal> getPsRatio(@Valid @RequestBody SummaryRequest request) {
        return valuationService.calculate(request, Metric.PS_RATIO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/ps/assessment")
    public ResponseEntity<MetricResponse> getPsRatioAssessment(@Valid @RequestBody SummaryRequest request) {
        return valuationService.assess(request, Metric.PS_RATIO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/peg")
    public ResponseEntity<BigDecimal> getPegRatio(@Valid @RequestBody SummaryRequest request) {
        return valuationService.calculate(request, Metric.PEG_RATIO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/peg/assessment")
    public ResponseEntity<MetricResponse> getPegRatioAssessment(@Valid @RequestBody SummaryRequest request) {
        return valuationService.assess(request, Metric.PEG_RATIO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/de")
    public ResponseEntity<BigDecimal> getDeRatio(@Valid @RequestBody SummaryRequest request) {
        return valuationService.calculate(request, Metric.DE_RATIO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/de/assessment")
    public ResponseEntity<MetricResponse> getDeRatioAssessment(@Valid @RequestBody SummaryRequest request) {
        return valuationService.assess(request, Metric.DE_RATIO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/roe")
    public ResponseEntity<BigDecimal> getRoe(@Valid @RequestBody SummaryRequest request) {
        return valuationService.calculate(request, Metric.ROE)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/roe/assessment")
    public ResponseEntity<MetricResponse> getRoeAssessment(@Valid @RequestBody SummaryRequest request) {
        return valuationService.assess(request, Metric.ROE)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/graham")
    public ResponseEntity<BigDecimal> getGrahamNumber(@Valid @RequestBody SummaryRequest request) {
        return valuationService.calculate(request, Metric.GRAHAM_NUMBER)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/graham/assessment")
    public ResponseEntity<MetricResponse> getGrahamNumberAssessment(@Valid @RequestBody SummaryRequest request) {
        return valuationService.assess(request, Metric.GRAHAM_NUMBER)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/lynch")
    public ResponseEntity<BigDecimal> getLynchFairValue(@Valid @RequestBody SummaryRequest request) {
        return valuationService.calculate(request, Metric.LYNCH_FAIR_VALUE)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/lynch/assessment")
    public ResponseEntity<MetricResponse> getLynchFairValueAssessment(@Valid @RequestBody SummaryRequest request) {
        return valuationService.assess(request, Metric.LYNCH_FAIR_VALUE)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.unprocessableContent().build());
    }

    @PostMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummaryAssessment(@Valid @RequestBody SummaryRequest request) {
        return ResponseEntity.ok(valuationService.assessSummary(request));
    }
}
