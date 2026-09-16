package finance

import (
	"math"
	"testing"
)

func ptr(v float64) *float64 { return &v }

// approxEqual compares floats with tolerance, since intermediate values like
// 2.4 aren't exactly representable in binary floating point — a literal
// equality check (==) can fail on a result that's mathematically correct.
func approxEqual(a, b float64) bool {
	return math.Abs(a-b) < 1e-9
}

func TestComputeMetrics_BasicRatios(t *testing.T) {
	f := Fundamentals{
		NetIncomeLoss:      ptr(100),
		StockholdersEquity: ptr(500),
		Revenue:            ptr(1000),
		SharesOutstanding:  ptr(50),
	}

	m := ComputeMetrics(f)

	if m.EPS == nil || *m.EPS != 2 {
		t.Errorf("expected EPS 2, got %v", m.EPS)
	}
	if m.BVPS == nil || *m.BVPS != 10 {
		t.Errorf("expected BVPS 10, got %v", m.BVPS)
	}
	if m.ROE == nil || *m.ROE != 0.2 {
		t.Errorf("expected ROE 0.2, got %v", m.ROE)
	}
	if m.NetMargin == nil || *m.NetMargin != 0.1 {
		t.Errorf("expected NetMargin 0.1, got %v", m.NetMargin)
	}
}

func TestComputeMetrics_EPSGrowthRateUsesCurrentSharesForBothYears(t *testing.T) {
	f := Fundamentals{
		NetIncomeLoss:      ptr(120),
		NetIncomeLossPrior: ptr(100),
		SharesOutstanding:  ptr(50), // same share count used for eps and eps_prior
	}

	m := ComputeMetrics(f)

	// eps = 2.4, eps_prior = 2.0, growth = (2.4-2.0)/2.0 = 0.2
	if m.EPSGrowthRate == nil || !approxEqual(*m.EPSGrowthRate, 0.2) {
		t.Errorf("expected EPSGrowthRate 0.2, got %v", m.EPSGrowthRate)
	}
}

func TestComputeMetrics_NilWhenInputMissing(t *testing.T) {
	f := Fundamentals{NetIncomeLoss: ptr(100)} // SharesOutstanding, StockholdersEquity, Revenue all nil

	m := ComputeMetrics(f)

	if m.EPS != nil {
		t.Errorf("expected nil EPS when SharesOutstanding is missing, got %v", *m.EPS)
	}
	if m.BVPS != nil {
		t.Errorf("expected nil BVPS when StockholdersEquity is missing, got %v", *m.BVPS)
	}
	if m.ROE != nil {
		t.Errorf("expected nil ROE when StockholdersEquity is missing, got %v", *m.ROE)
	}
	if m.NetMargin != nil {
		t.Errorf("expected nil NetMargin when Revenue is missing, got %v", *m.NetMargin)
	}
}

func TestComputeMetrics_NilWhenDenominatorZero(t *testing.T) {
	f := Fundamentals{
		NetIncomeLoss:     ptr(100),
		SharesOutstanding: ptr(0),
	}

	m := ComputeMetrics(f)

	if m.EPS != nil {
		t.Errorf("expected nil EPS for a zero share count, got %v", *m.EPS)
	}
}

func TestComputeMetrics_EPSGrowthRateNilWhenPriorMissing(t *testing.T) {
	f := Fundamentals{
		NetIncomeLoss:     ptr(120),
		SharesOutstanding: ptr(50),
		// NetIncomeLossPrior is nil
	}

	m := ComputeMetrics(f)

	if m.EPSGrowthRate != nil {
		t.Errorf("expected nil EPSGrowthRate when prior net income is missing, got %v", *m.EPSGrowthRate)
	}
}

// TestComputeMetrics_GrowthFromNegativePriorEPSIsNotMeaningful pins a known,
// deliberately-unfixed quirk of the plan's growth formula: when eps_prior is
// negative (a loss year) and eps turns positive, (eps-eps_prior)/eps_prior
// comes out negative even though the company improved. Flagged to the user;
// not silently special-cased here.
func TestComputeMetrics_GrowthFromNegativePriorEPSIsNotMeaningful(t *testing.T) {
	f := Fundamentals{
		NetIncomeLoss:      ptr(50),  // profitable this year
		NetIncomeLossPrior: ptr(-50), // loss last year
		SharesOutstanding:  ptr(50),
	}

	m := ComputeMetrics(f)

	// eps = 1, eps_prior = -1, growth = (1 - (-1)) / -1 = -2
	if m.EPSGrowthRate == nil || *m.EPSGrowthRate != -2 {
		t.Errorf("expected the known negative-base artifact (-2), got %v", m.EPSGrowthRate)
	}
}
