package main

import (
	"context"
	"fmt"
	"log/slog"
	"strconv"

	"github.com/UmutAkbayin/fametrics/screener/db"
	"github.com/UmutAkbayin/fametrics/screener/finance"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
)

// persistCandidates upserts every candidate into companies (passing and
// failing alike — passes_hard_filter is what step 7's endpoint filters on)
// and records one ingestion_runs row. The whole run is one transaction, but
// each company is upserted inside its own savepoint: a row that fails (e.g.
// a value too large for its column, like a subsidiary filer with almost no
// public float) is rolled back and skipped individually, without discarding
// every other company already upserted in this run.
func persistCandidates(ctx context.Context, pool *pgxpool.Pool, candidates []finance.Candidate, quarter string) error {
	tx, err := pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback(ctx) // no-op once Commit has succeeded

	upserted, skipped := 0, 0
	for _, c := range candidates {
		cik, err := strconv.ParseInt(c.CIK, 10, 64)
		if err != nil {
			slog.Warn("skipping candidate with unparseable CIK", "cik", c.CIK, "error", err)
			skipped++
			continue
		}

		if err := upsertOne(ctx, tx, cik, c); err != nil {
			slog.Warn("skipping company that failed to upsert", "cik", cik, "error", err)
			skipped++
			continue
		}
		upserted++
	}
	slog.Info("upserted companies", "upserted", upserted, "skipped", skipped)

	if err := db.New(tx).InsertIngestionRun(ctx, db.InsertIngestionRunParams{
		Quarter:      quarter,
		CompanyCount: int32(upserted),
	}); err != nil {
		return fmt.Errorf("failed to record ingestion run: %w", err)
	}

	return tx.Commit(ctx)
}

// upsertOne upserts a single company inside its own savepoint — pgx issues a
// real SQL SAVEPOINT when Begin is called on an already-open Tx. If
// UpsertCompany fails, only this savepoint rolls back; everything upserted
// earlier in tx stays intact and can still be committed.
func upsertOne(ctx context.Context, tx pgx.Tx, cik int64, c finance.Candidate) error {
	savepoint, err := tx.Begin(ctx)
	if err != nil {
		return err
	}
	defer savepoint.Rollback(ctx) // no-op once Commit has succeeded

	if err := db.New(savepoint).UpsertCompany(ctx, toUpsertCompanyParams(cik, c)); err != nil {
		return err
	}
	return savepoint.Commit(ctx)
}

func toUpsertCompanyParams(cik int64, c finance.Candidate) db.UpsertCompanyParams {
	f := c.Fundamentals
	m := c.Metrics
	return db.UpsertCompanyParams{
		Cik:                    cik,
		Name:                   c.Name,
		PeriodEnd:              pgtype.Date{Time: c.Period, Valid: true},
		Adsh:                   c.ADSH,
		Assets:                 f.Assets,
		Liabilities:            f.Liabilities,
		StockholdersEquity:     f.StockholdersEquity,
		NetIncome:              f.NetIncomeLoss,
		OperatingIncome:        f.OperatingIncomeLoss,
		OperatingCashFlow:      f.OperatingCashFlow,
		Capex:                  f.Capex,
		Revenue:                f.Revenue,
		Cash:                   f.Cash,
		LongTermDebt:           f.LongTermDebt,
		SharesOutstanding:      f.SharesOutstanding,
		OperatingCashFlowPrior: f.OperatingCashFlowPrior,
		CapexPrior:             f.CapexPrior,
		NetIncomePrior:         f.NetIncomeLossPrior,
		Eps:                    m.EPS,
		Bvps:                   m.BVPS,
		EpsGrowthRate:          m.EPSGrowthRate,
		QualityScore:           c.QualityScore,
		PassesHardFilter:       c.PassesHardFilter,
	}
}
