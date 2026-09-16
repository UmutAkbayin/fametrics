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
	m.EPS = ratio(f.NetIncomeLoss, f.SharesOutstanding)
	m.BVPS = ratio(f.StockholdersEquity, f.SharesOutstanding)
	m.ROE = ratio(f.NetIncomeLoss, f.StockholdersEquity)
	m.NetMargin = ratio(f.NetIncomeLoss, f.Revenue)

	// eps_growth_rate uses the current year's share count for both years'
	// EPS, per the plan: only the net income numerator changes.
	epsPrior := ratio(f.NetIncomeLossPrior, f.SharesOutstanding)
	m.EPSGrowthRate = ratio(subtract(m.EPS, epsPrior), epsPrior)

	return m
}

// ratio safely divides two optional values: nil if either input is missing
// or the denominator is zero.
func ratio(numerator, denominator *float64) *float64 {
	if numerator == nil || denominator == nil || *denominator == 0 {
		return nil
	}
	result := *numerator / *denominator
	return &result
}

// subtract safely subtracts two optional values: nil if either is missing.
func subtract(a, b *float64) *float64 {
	if a == nil || b == nil {
		return nil
	}
	result := *a - *b
	return &result
}
