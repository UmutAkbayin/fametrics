package main

import (
	"context"
	"testing"
	"time"

	"github.com/UmutAkbayin/fametrics/screener/finance"
)

func f64(v float64) *float64 { return &v }

func healthyCandidate(cik, name, adsh string) finance.Candidate {
	return finance.Candidate{
		CIK: cik, Name: name, ADSH: adsh,
		Period:           mustParseDate("20251231"),
		PassesHardFilter: true,
		QualityScore:     f64(75),
		Fundamentals: finance.Fundamentals{
			Assets: f64(1000), StockholdersEquity: f64(500), NetIncomeLoss: f64(100),
		},
		Metrics: finance.Metrics{ROE: f64(0.2), NetMargin: f64(0.1)},
	}
}

func mustParseDate(s string) time.Time {
	t, err := time.Parse("20060102", s)
	if err != nil {
		panic(err)
	}
	return t
}

func TestPersistCandidates_UpsertInsertsThenUpdatesOnConflict(t *testing.T) {
	truncateTables(t)
	ctx := context.Background()

	c := healthyCandidate("1", "ACME INC", "0001-1")
	if err := persistCandidates(ctx, testPool, []finance.Candidate{c}, "2026q2"); err != nil {
		t.Fatalf("first persist: %v", err)
	}

	var name string
	var assets *float64
	if err := testPool.QueryRow(ctx, "select name, assets from companies where cik=$1", int64(1)).Scan(&name, &assets); err != nil {
		t.Fatalf("query: %v", err)
	}
	if name != "ACME INC" || assets == nil || *assets != 1000 {
		t.Fatalf("unexpected row after insert: name=%q assets=%v", name, assets)
	}

	// Same CIK, changed fields — should update in place via ON CONFLICT, not insert a second row.
	c.Name = "ACME INCORPORATED"
	c.Fundamentals.Assets = f64(2000)
	if err := persistCandidates(ctx, testPool, []finance.Candidate{c}, "2026q2"); err != nil {
		t.Fatalf("second persist: %v", err)
	}

	var count int
	if err := testPool.QueryRow(ctx, "select count(*) from companies where cik=$1", int64(1)).Scan(&count); err != nil {
		t.Fatalf("count query: %v", err)
	}
	if count != 1 {
		t.Errorf("expected exactly 1 row after upsert, got %d", count)
	}

	if err := testPool.QueryRow(ctx, "select name, assets from companies where cik=$1", int64(1)).Scan(&name, &assets); err != nil {
		t.Fatalf("query after update: %v", err)
	}
	if name != "ACME INCORPORATED" || assets == nil || *assets != 2000 {
		t.Errorf("expected the row to be updated in place, got name=%q assets=%v", name, assets)
	}
}

func TestPersistCandidates_MissingFundamentalsPersistAsNull(t *testing.T) {
	truncateTables(t)
	ctx := context.Background()

	c := finance.Candidate{
		CIK: "2", Name: "NO DATA CO", ADSH: "0002-1",
		Period:           mustParseDate("20251231"),
		PassesHardFilter: false,
		// Fundamentals and Metrics left zero-value: every *float64 is nil.
	}

	if err := persistCandidates(ctx, testPool, []finance.Candidate{c}, "2026q2"); err != nil {
		t.Fatalf("persist: %v", err)
	}

	var assets, qualityScore *float64
	var passes bool
	err := testPool.QueryRow(ctx, "select assets, quality_score, passes_hard_filter from companies where cik=$1", int64(2)).
		Scan(&assets, &qualityScore, &passes)
	if err != nil {
		t.Fatalf("query: %v", err)
	}
	if assets != nil || qualityScore != nil || passes {
		t.Errorf("expected NULL fundamentals/quality_score and passes=false, got assets=%v quality_score=%v passes=%v", assets, qualityScore, passes)
	}
}

// TestPersistCandidates_BadRowIsSkippedWithoutLosingTheRest is a regression
// test for the real bug this savepoint design fixed: BERKSHIRE HATHAWAY
// ENERGY's bvps overflowed NUMERIC(14,4) (since widened) and aborted the
// entire batch. A single company whose value is too large for its column
// must be skipped, not take the other companies in the run down with it.
func TestPersistCandidates_BadRowIsSkippedWithoutLosingTheRest(t *testing.T) {
	truncateTables(t)
	ctx := context.Background()

	good := healthyCandidate("1", "GOOD CO", "0001-1")

	bad := healthyCandidate("2", "OVERFLOW CO", "0002-1")
	bad.Metrics.BVPS = f64(1e30) // exceeds NUMERIC(20,4)'s ~10^16 headroom

	if err := persistCandidates(ctx, testPool, []finance.Candidate{good, bad}, "2026q2"); err != nil {
		t.Fatalf("expected persistCandidates to tolerate one bad row, got error: %v", err)
	}

	var goodCount, badCount int
	if err := testPool.QueryRow(ctx, "select count(*) from companies where cik=$1", int64(1)).Scan(&goodCount); err != nil {
		t.Fatalf("count good: %v", err)
	}
	if err := testPool.QueryRow(ctx, "select count(*) from companies where cik=$1", int64(2)).Scan(&badCount); err != nil {
		t.Fatalf("count bad: %v", err)
	}
	if goodCount != 1 {
		t.Errorf("expected the healthy company to be persisted, got count=%d", goodCount)
	}
	if badCount != 0 {
		t.Errorf("expected the overflowing company to be skipped, got count=%d", badCount)
	}

	var runCount int
	if err := testPool.QueryRow(ctx, "select company_count from ingestion_runs order by id desc limit 1").Scan(&runCount); err != nil {
		t.Fatalf("query ingestion_runs: %v", err)
	}
	if runCount != 1 {
		t.Errorf("expected ingestion_runs.company_count to reflect only the successfully upserted row (1), got %d", runCount)
	}
}

func TestPersistCandidates_UnparseableCIKIsSkipped(t *testing.T) {
	truncateTables(t)
	ctx := context.Background()

	c := healthyCandidate("not-a-number", "BAD CIK CO", "0003-1")

	if err := persistCandidates(ctx, testPool, []finance.Candidate{c}, "2026q2"); err != nil {
		t.Fatalf("persist: %v", err)
	}

	var count int
	if err := testPool.QueryRow(ctx, "select count(*) from companies").Scan(&count); err != nil {
		t.Fatalf("count: %v", err)
	}
	if count != 0 {
		t.Errorf("expected the unparseable-CIK candidate to be skipped entirely, got %d rows", count)
	}
}
