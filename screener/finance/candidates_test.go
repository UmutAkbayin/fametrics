package finance

import "testing"

// healthyFundamentals is a baseline company that satisfies every hard
// filter, for tests to selectively break one field at a time from.
func healthyFundamentals() Fundamentals {
	return Fundamentals{
		StockholdersEquity:     ptr(1000),
		Liabilities:            ptr(500), // D/E = 0.5, under 1
		NetIncomeLoss:          ptr(150), // ROE = 0.15, over 0.10
		OperatingCashFlow:      ptr(300),
		Capex:                  ptr(100), // FCF = 200, positive
		OperatingCashFlowPrior: ptr(250),
		CapexPrior:             ptr(100), // prior FCF = 150, positive
		Revenue:                ptr(1000),
	}
}

func healthyCandidate() Candidate {
	f := healthyFundamentals()
	return Candidate{CIK: "1", Fundamentals: f, Metrics: ComputeMetrics(f)}
}

func TestPassesHardFilter_AllCriteriaMet(t *testing.T) {
	if !passesHardFilter(healthyCandidate()) {
		t.Error("expected a company meeting every criterion to pass")
	}
}

func TestPassesHardFilter_FailsWhenEquityNotPositive(t *testing.T) {
	c := healthyCandidate()
	c.Fundamentals.StockholdersEquity = ptr(0)
	c.Metrics = ComputeMetrics(c.Fundamentals)

	if passesHardFilter(c) {
		t.Error("expected zero equity to fail the filter")
	}
}

func TestPassesHardFilter_FailsWhenCurrentFCFNotPositive(t *testing.T) {
	c := healthyCandidate()
	c.Fundamentals.Capex = ptr(300) // current FCF = 300 - 300 = 0

	if passesHardFilter(c) {
		t.Error("expected non-positive current-year FCF to fail the filter")
	}
}

func TestPassesHardFilter_FailsWhenPriorFCFNotPositive(t *testing.T) {
	c := healthyCandidate()
	c.Fundamentals.CapexPrior = ptr(250) // prior FCF = 250 - 250 = 0

	if passesHardFilter(c) {
		t.Error("expected non-positive prior-year FCF to fail the filter")
	}
}

func TestPassesHardFilter_FailsWhenDebtToEquityTooHigh(t *testing.T) {
	c := healthyCandidate()
	c.Fundamentals.Liabilities = ptr(1000) // D/E = 1.0, not < 1

	if passesHardFilter(c) {
		t.Error("expected a debt-to-equity ratio of exactly 1 to fail the filter")
	}
}

func TestPassesHardFilter_FailsWhenROETooLow(t *testing.T) {
	c := healthyCandidate()
	c.Fundamentals.NetIncomeLoss = ptr(100) // ROE = 0.10, not > 0.10
	c.Metrics = ComputeMetrics(c.Fundamentals)

	if passesHardFilter(c) {
		t.Error("expected ROE of exactly 10% to fail the filter")
	}
}

func TestPassesHardFilter_FailsClosedWhenLiabilitiesMissing(t *testing.T) {
	c := healthyCandidate()
	c.Fundamentals.Liabilities = nil // e.g. the real filer we found that never tags Liabilities

	if passesHardFilter(c) {
		t.Error("expected a missing Liabilities value to fail the filter, not be treated as zero debt")
	}
}

func TestComputeQualityScores_RanksOnlyPassingCandidates(t *testing.T) {
	passingLow := healthyCandidate()
	passingLow.CIK = "low"
	passingLow.PassesHardFilter = true
	passingLow.Metrics.ROE = ptr(0.11)
	passingLow.Metrics.NetMargin = ptr(0.05)

	passingHigh := healthyCandidate()
	passingHigh.CIK = "high"
	passingHigh.PassesHardFilter = true
	passingHigh.Metrics.ROE = ptr(0.30)
	passingHigh.Metrics.NetMargin = ptr(0.25)

	failing := healthyCandidate()
	failing.CIK = "failing"
	failing.PassesHardFilter = false
	failing.Metrics.ROE = ptr(0.90) // would dominate the ranking if wrongly included
	failing.Metrics.NetMargin = ptr(0.90)

	candidates := []Candidate{passingLow, passingHigh, failing}
	ComputeQualityScores(candidates)

	if candidates[2].QualityScore != nil {
		t.Errorf("expected no QualityScore for a candidate that failed the hard filter, got %v", *candidates[2].QualityScore)
	}
	if candidates[0].QualityScore == nil || *candidates[0].QualityScore != 50 {
		t.Errorf("expected the lower-ranked passer to score 50 (1 of 2), got %v", candidates[0].QualityScore)
	}
	if candidates[1].QualityScore == nil || *candidates[1].QualityScore != 100 {
		t.Errorf("expected the higher-ranked passer to score 100 (2 of 2), got %v", candidates[1].QualityScore)
	}
}

func TestComputeQualityScores_NoPassingCandidatesLeavesAllNil(t *testing.T) {
	c := healthyCandidate()
	c.PassesHardFilter = false
	candidates := []Candidate{c}

	ComputeQualityScores(candidates)

	if candidates[0].QualityScore != nil {
		t.Errorf("expected nil QualityScore when nothing passed the hard filter, got %v", *candidates[0].QualityScore)
	}
}
