package finance

// Metrics holds derived, price-independent per-company ratios computed from
// Fundamentals. A field is nil when an input it depends on is itself nil, or
// the ratio's denominator is zero. quality_score is deliberately not
// computed here: it's a percentile rank across the surviving candidates
// after the hard filter, not a value one company's Fundamentals can produce
// on its own.
type Metrics struct {
	EPS           *float64
	BVPS          *float64
	EPSGrowthRate *float64
	ROE           *float64
	NetMargin     *float64
}

// ComputeMetrics derives EPS, BVPS, EPS growth, ROE, and net margin from one
// company's Fundamentals. ROE and NetMargin aren't named in step 4's field
// list, but they're the same kind of price-independent per-company ratio and
// are needed downstream (ROE by the hard filter, both by quality_score), so
// they're computed once here instead of being recomputed at each later use.
func ComputeMetrics(f Fundamentals) Metrics {
	var m Metrics
	m.EPS = positiveRatio(f.NetIncomeLoss, f.SharesOutstanding)
	m.BVPS = positiveRatio(f.StockholdersEquity, f.SharesOutstanding)
	m.ROE = positiveRatio(f.NetIncomeLoss, f.StockholdersEquity)
	m.NetMargin = positiveRatio(f.NetIncomeLoss, f.Revenue)

	// eps_growth_rate uses the current year's share count for both years'
	// EPS, per the plan: only the net income numerator changes. The share
	// count denominator here still must be positive; a negative eps_prior
	// (a loss year) is fine and expected, it's the numerator of this step,
	// not the denominator.
	epsPrior := positiveRatio(f.NetIncomeLossPrior, f.SharesOutstanding)
	// eps_prior becomes the denominator below, and it's allowed to be
	// negative (see TestComputeMetrics_GrowthFromNegativePriorEPSIsNotMeaningful),
	// so this uses plain ratio, not positiveRatio.
	m.EPSGrowthRate = ratio(subtract(m.EPS, epsPrior), epsPrior)

	return m
}

// ratio safely divides two optional values: nil if either input is missing
// or the denominator is zero. The denominator may be negative — used where
// a negative base is mathematically valid (see eps_growth_rate above).
func ratio(numerator, denominator *float64) *float64 {
	if numerator == nil || denominator == nil || *denominator == 0 {
		return nil
	}
	result := *numerator / *denominator
	return &result
}

// positiveRatio is ratio, but also rejects a negative (not just zero)
// denominator. Used for per-share and equity-based ratios (EPS, BVPS, ROE,
// NetMargin), where a negative denominator — negative equity, negative
// shares outstanding — would flip the sign of the result and produce a
// misleadingly "good-looking" number for what is actually a distressed
// company: a net loss divided by negative equity comes out positive. This
// mirrors core's (Java) RoeAssessor/DeRatioAssessor convention of requiring
// totalEquity.signum() > 0 before dividing by it.
func positiveRatio(numerator, denominator *float64) *float64 {
	if denominator == nil || *denominator <= 0 {
		return nil
	}
	return ratio(numerator, denominator)
}

// subtract safely subtracts two optional values: nil if either is missing.
func subtract(a, b *float64) *float64 {
	if a == nil || b == nil {
		return nil
	}
	result := *a - *b
	return &result
}
