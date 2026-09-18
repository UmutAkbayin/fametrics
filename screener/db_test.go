package main

import (
	"context"
	"errors"
	"testing"

	"github.com/UmutAkbayin/fametrics/screener/db"
	"github.com/UmutAkbayin/fametrics/screener/finance"
	"github.com/jackc/pgx/v5"
)

func TestListCandidates_OnlyPassingOrderedByQualityScoreNullsLast(t *testing.T) {
	truncateTables(t)
	ctx := context.Background()

	low := healthyCandidate("1", "LOW SCORE CO", "0001-1")
	low.QualityScore = f64(40)

	high := healthyCandidate("2", "HIGH SCORE CO", "0002-1")
	high.QualityScore = f64(90)

	// Passes the hard filter but has no quality_score (e.g. net_margin
	// couldn't be computed) — must sort last, not first.
	unranked := healthyCandidate("3", "UNRANKED CO", "0003-1")
	unranked.QualityScore = nil

	failing := healthyCandidate("4", "FAILING CO", "0004-1")
	failing.PassesHardFilter = false
	failing.QualityScore = f64(99) // would dominate the ranking if wrongly included

	all := []finance.Candidate{low, high, unranked, failing}
	if err := persistCandidates(ctx, testPool, all, "2026q2"); err != nil {
		t.Fatalf("persist: %v", err)
	}

	got, err := db.New(testPool).ListCandidates(ctx, 10)
	if err != nil {
		t.Fatalf("ListCandidates: %v", err)
	}

	if len(got) != 3 {
		t.Fatalf("expected 3 passing candidates, got %d", len(got))
	}
	wantOrder := []string{"HIGH SCORE CO", "LOW SCORE CO", "UNRANKED CO"}
	for i, want := range wantOrder {
		if got[i].Name != want {
			t.Errorf("position %d: expected %q, got %q", i, want, got[i].Name)
		}
	}
}

func TestListCandidates_RespectsLimit(t *testing.T) {
	truncateTables(t)
	ctx := context.Background()

	a := healthyCandidate("1", "A CO", "0001-1")
	b := healthyCandidate("2", "B CO", "0002-1")
	if err := persistCandidates(ctx, testPool, []finance.Candidate{a, b}, "2026q2"); err != nil {
		t.Fatalf("persist: %v", err)
	}

	got, err := db.New(testPool).ListCandidates(ctx, 1)
	if err != nil {
		t.Fatalf("ListCandidates: %v", err)
	}
	if len(got) != 1 {
		t.Errorf("expected limit=1 to return exactly 1 row, got %d", len(got))
	}
}

func TestGetLatestIngestionQuarter_NoRowsReturnsErrNoRows(t *testing.T) {
	truncateTables(t)

	_, err := db.New(testPool).GetLatestIngestionQuarter(context.Background())
	if !errors.Is(err, pgx.ErrNoRows) {
		t.Errorf("expected pgx.ErrNoRows on an empty table, got %v", err)
	}
}

func TestGetLatestIngestionQuarter_ReturnsTheMostRecentRun(t *testing.T) {
	truncateTables(t)
	ctx := context.Background()

	if err := persistCandidates(ctx, testPool, nil, "2025q4"); err != nil {
		t.Fatalf("persist run 1: %v", err)
	}
	if err := persistCandidates(ctx, testPool, nil, "2026q1"); err != nil {
		t.Fatalf("persist run 2: %v", err)
	}

	got, err := db.New(testPool).GetLatestIngestionQuarter(ctx)
	if err != nil {
		t.Fatalf("GetLatestIngestionQuarter: %v", err)
	}
	if got != "2026q1" {
		t.Errorf("expected the most recently inserted run's quarter (2026q1), got %q", got)
	}
}
