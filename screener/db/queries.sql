-- name: UpsertCompany :exec
INSERT INTO companies (
    cik, name, period_end, adsh,
    assets, liabilities, stockholders_equity,
    net_income, operating_income, operating_cash_flow, capex, revenue, cash, long_term_debt, shares_outstanding,
    operating_cash_flow_prior, capex_prior, net_income_prior,
    eps, bvps, eps_growth_rate, quality_score,
    passes_hard_filter, updated_at
) VALUES (
    $1, $2, $3, $4,
    $5, $6, $7,
    $8, $9, $10, $11, $12, $13, $14, $15,
    $16, $17, $18,
    $19, $20, $21, $22,
    $23, now()
)
ON CONFLICT (cik) DO UPDATE SET
    name = EXCLUDED.name,
    period_end = EXCLUDED.period_end,
    adsh = EXCLUDED.adsh,
    assets = EXCLUDED.assets,
    liabilities = EXCLUDED.liabilities,
    stockholders_equity = EXCLUDED.stockholders_equity,
    net_income = EXCLUDED.net_income,
    operating_income = EXCLUDED.operating_income,
    operating_cash_flow = EXCLUDED.operating_cash_flow,
    capex = EXCLUDED.capex,
    revenue = EXCLUDED.revenue,
    cash = EXCLUDED.cash,
    long_term_debt = EXCLUDED.long_term_debt,
    shares_outstanding = EXCLUDED.shares_outstanding,
    operating_cash_flow_prior = EXCLUDED.operating_cash_flow_prior,
    capex_prior = EXCLUDED.capex_prior,
    net_income_prior = EXCLUDED.net_income_prior,
    eps = EXCLUDED.eps,
    bvps = EXCLUDED.bvps,
    eps_growth_rate = EXCLUDED.eps_growth_rate,
    quality_score = EXCLUDED.quality_score,
    passes_hard_filter = EXCLUDED.passes_hard_filter,
    updated_at = now();

-- name: InsertIngestionRun :exec
INSERT INTO ingestion_runs (quarter, company_count)
VALUES ($1, $2);
