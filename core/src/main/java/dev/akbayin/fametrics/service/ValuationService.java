package dev.akbayin.fametrics.service;

import dev.akbayin.fametrics.domain.Metric;
import dev.akbayin.fametrics.domain.MetricAssessor;
import dev.akbayin.fametrics.dto.MetricResponse;
import dev.akbayin.fametrics.dto.SummaryRequest;
import dev.akbayin.fametrics.dto.SummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ValuationService {

    private final Map<Metric, MetricAssessor> assessorMap;

    public ValuationService(List<MetricAssessor> assessors) {
        this.assessorMap = assessors.stream()
            .collect(Collectors.toUnmodifiableMap(
                MetricAssessor::metric,
                Function.identity()
            ));
    }

    public SummaryResponse assessSummary(SummaryRequest request) {
        List<MetricResponse> metrics = Arrays.stream(Metric.values())
            .map(metric -> assess(request, metric))
            .flatMap(Optional::stream)
            .toList();
        return new SummaryResponse(metrics);
    }

    public Optional<BigDecimal> calculate(SummaryRequest request, Metric metric) {
        MetricAssessor assessor = assessorMap.get(metric);
        if (assessor == null) {
            log.warn("No assessor registered for metric {}", metric);
            return Optional.empty();
        }
        return assessor.calculate(request);
    }

    public Optional<MetricResponse> assess(SummaryRequest request, Metric metric) {
        MetricAssessor assessor = assessorMap.get(metric);
        if (assessor == null) {
            log.warn("No assessor registered for metric {}", metric);
            return Optional.empty();
        }

        return assessor.calculate(request)
            .map(value -> {
                var evaluation = assessor.evaluate(request, value);

                return new MetricResponse(
                    assessor.metric(),
                    value,
                    evaluation.assessment(),
                    evaluation.benchmark(),
                    assessor.description(),
                    assessor.interpretation(request, value, evaluation.assessment())
                );
            });
    }
}
