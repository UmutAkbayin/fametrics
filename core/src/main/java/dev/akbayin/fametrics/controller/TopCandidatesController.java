package dev.akbayin.fametrics.controller;

import dev.akbayin.fametrics.dto.TopCandidateResponse;
import dev.akbayin.fametrics.ranking.TopCandidateMapper;
import dev.akbayin.fametrics.ranking.TopCandidatesService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/candidates")
public class TopCandidatesController {

    private static final int DEFAULT_LIMIT = 20;

    private final TopCandidatesService topCandidatesService;

    /**
     * The screener's own quality_score plus core's Value Score, combined
     * and sorted — what the frontend calls. Always computed fresh: no
     * caching or change-detection on this endpoint itself (the screener
     * candidates and FMP prices behind it are what's cached — see
     * PriceCache — this endpoint just recombines whatever's current).
     */
    @GetMapping("/top")
    public ResponseEntity<List<TopCandidateResponse>> getTopCandidates(
        @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit
    ) {
        List<TopCandidateResponse> response = topCandidatesService.getTopCandidates(limit).stream()
            .map(TopCandidateMapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }
}
