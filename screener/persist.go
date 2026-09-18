package main

import (
	"context"
	"fmt"
	"log/slog"
	"strconv"

	"github.com/UmutAkbayin/fametrics/screener/db"
	"github.com/UmutAkbayin/fametrics/screener/finance"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
)

// persistCandidates upserts every candidate into companies (passing and
// failing alike — passes_hard_filter is what step 7's endpoint filters on)
// and records one ingestion_runs row, all inside a single transaction so a
// failure partway through doesn't leave the database with a partial update.
func persistCandidates(ctx context.Context, pool *pgxpool.Pool, candidates []finance.Candidate, quarter string) error {
	tx, err := pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback(ctx) // no-op once Commit has succeeded

	queries := db.New(tx)

	for _, c := range candidates {
		cik, err := strconv.ParseInt(c.CIK, 10, 64)
		if err != nil {
			slog.Warn("skipping candidate with unparseable CIK", "cik", c.CIK, "error", err)
			continue
		}

		if err := queries.UpsertCompany(ctx, toUpsertCompanyParams(cik, c)); err != nil {
			return fmt.Errorf("failed to upsert company %d: %w", cik, err)
		}
	}

	if err := queries.InsertIngestionRun(ctx, db.InsertIngestionRunParams{
		Quarter:      quarter,
		CompanyCount: int32(len(candidates)),
	}); err != nil {
		return fmt.Errorf("failed to record ingestion run: %w", err)
	}

	return tx.Commit(ctx)
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
