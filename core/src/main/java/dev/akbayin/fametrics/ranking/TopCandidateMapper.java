package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.dto.TopCandidateResponse;

/**
 * Builds the public {@link TopCandidateResponse} from an internal {@link
 * FinalCandidate}. Kept separate from the dto package (which otherwise holds
 * only plain data records, no logic) and from FinalCandidate itself (which
 * shouldn't need to know about the public response shape).
 */
public final class TopCandidateMapper {

    private TopCandidateMapper() {
    }

    public static TopCandidateResponse toResponse(FinalCandidate finalCandidate) {
        var candidate = finalCandidate.ranked().assessed().candidate();
        var price = finalCandidate.ranked().assessed().price();

        return new TopCandidateResponse(
            candidate.cik(),
            candidate.name(),
            price.ticker(),
            candidate.periodEnd(),
            price.price(),
            candidate.qualityScore(),
            finalCandidate.ranked().valueScore(),
            finalCandidate.finalScore(),
            finalCandidate.ranked().assessed().summary().metrics()
        );
    }
}
