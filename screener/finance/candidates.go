package finance

import "time"

// Candidate joins one company's submission identity, raw fundamentals, and
// derived metrics — the shape the hard filter and quality score are computed
// from.
type Candidate struct {
	CIK    string
	Name   string
	ADSH   string
	Period time.Time

	Fundamentals Fundamentals
	Metrics      Metrics

	PassesHardFilter bool
	// QualityScore is nil until ComputeQualityScores runs, and stays nil for
	// any candidate that doesn't pass the hard filter — it ranks the
	// value-investing shortlist, not the full universe of filings.
	QualityScore *float64
}

// BuildCandidates joins submissions (keyed by CIK, as latestTenKByCIK
// produces), fundamentals, and metrics (both keyed by ADSH, as
// ExtractFundamentals and the metrics computation loop produce) into one
// Candidate per company.
func BuildCandidates(submissions map[string]Submission, fundamentals map[string]Fundamentals, metrics map[string]Metrics) []Candidate {
	candidates := make([]Candidate, 0, len(submissions))
	for cik, sub := range submissions {
		candidates = append(candidates, Candidate{
			CIK:          cik,
			Name:         sub.Name,
			ADSH:         sub.ADSH,
			Period:       sub.Period,
			Fundamentals: fundamentals[sub.ADSH],
			Metrics:      metrics[sub.ADSH],
		})
	}
	return candidates
}

// ApplyHardFilters sets PassesHardFilter on each candidate. A candidate
// passes only if every criterion can be verified AND satisfied: positive
// equity, two years of positive free cash flow, a debt-to-equity ratio under
// 1, and ROE above 10%. A candidate missing a required input fails closed —
// "we can't verify this" is not the same as "this passed", so it counts as
// not passing rather than being silently waived through.
func ApplyHardFilters(candidates []Candidate) {
	for i := range candidates {
		candidates[i].PassesHardFilter = passesHardFilter(candidates[i])
	}
}

func passesHardFilter(c Candidate) bool {
	f := c.Fundamentals

	if f.StockholdersEquity == nil || *f.StockholdersEquity <= 0 {
		return false
	}
	if !positiveFreeCashFlow(f.OperatingCashFlow, f.Capex) {
		return false
	}
	if !positiveFreeCashFlow(f.OperatingCashFlowPrior, f.CapexPrior) {
		return false
	}

	debtToEquity := positiveRatio(f.Liabilities, f.StockholdersEquity)
	if debtToEquity == nil || *debtToEquity >= 1 {
		return false
	}

	if c.Metrics.ROE == nil || *c.Metrics.ROE <= 0.10 {
		return false
	}

	return true
}

// positiveFreeCashFlow reports whether operatingCashFlow-capex is positive.
// Both inputs must be present; a missing one fails the check, it is never
// treated as zero.
func positiveFreeCashFlow(operatingCashFlow, capex *float64) bool {
	fcf := subtract(operatingCashFlow, capex)
	return fcf != nil && *fcf > 0
}

// ComputeQualityScores assigns QualityScore (0-100) to every candidate with
// PassesHardFilter==true: the average of its percentile rank on ROE and on
// NetMargin among the other passing candidates. Candidates that don't pass
// the hard filter are excluded from the ranking population entirely, not
// just left unscored — they shouldn't pull the percentile curve for the
// companies that did pass.
func ComputeQualityScores(candidates []Candidate) {
	var passing []int
	for i, c := range candidates {
		if c.PassesHardFilter {
			passing = append(passing, i)
		}
	}

	roeRanks := percentileRanks(passing, candidates, func(c Candidate) *float64 { return c.Metrics.ROE })
	marginRanks := percentileRanks(passing, candidates, func(c Candidate) *float64 { return c.Metrics.NetMargin })

	for _, i := range passing {
		roeP, hasROE := roeRanks[i]
		marginP, hasMargin := marginRanks[i]
		if !hasROE || !hasMargin {
			continue // missing an input this ranking depends on; leave QualityScore nil
		}
		score := (roeP + marginP) / 2
		candidates[i].QualityScore = &score
	}
}

// percentileRanks computes, for each index in indices whose value(candidate)
// is non-nil, its percentile rank (0-100): the percentage of the other
// indices with a non-nil value whose value is at or below it. Indices with a
// nil value are excluded from the ranking population entirely, not
// penalized with a low rank.
func percentileRanks(indices []int, candidates []Candidate, value func(Candidate) *float64) map[int]float64 {
	type point struct {
		index int
		value float64
	}
	var points []point
	for _, i := range indices {
		if v := value(candidates[i]); v != nil {
			points = append(points, point{index: i, value: *v})
		}
	}

	ranks := make(map[int]float64, len(points))
	for _, p := range points {
		atOrBelow := 0
		for _, other := range points {
			if other.value <= p.value {
				atOrBelow++
			}
		}
		ranks[p.index] = float64(atOrBelow) / float64(len(points)) * 100
	}
	return ranks
}
