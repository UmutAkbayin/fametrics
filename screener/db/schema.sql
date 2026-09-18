create table companies
(
    cik                       BIGINT primary key,
    name                      TEXT        not null,

    period_end                DATE        not null,
    adsh                      TEXT        not null,

    assets                    NUMERIC(28, 4),
    liabilities               NUMERIC(28, 4),
    stockholders_equity       NUMERIC(28, 4),
    net_income                NUMERIC(28, 4),
    operating_income          NUMERIC(28, 4),
    operating_cash_flow       NUMERIC(28, 4),
    capex                     NUMERIC(28, 4),
    revenue                   NUMERIC(28, 4),
    cash                      NUMERIC(28, 4),
    long_term_debt            NUMERIC(28, 4),
    shares_outstanding        NUMERIC(20, 4),


    operating_cash_flow_prior NUMERIC(28, 4),
    capex_prior               NUMERIC(28, 4),
    net_income_prior          NUMERIC(28, 4),

    eps                       NUMERIC(20, 4),
    bvps                      NUMERIC(20, 4),
    eps_growth_rate           NUMERIC(10, 4),
    quality_score             NUMERIC(6, 2),

    passes_hard_filter        BOOLEAN     not null default false,

    updated_at                TIMESTAMPTZ not null default now()
);

create index idx_companies_hard_filter_quality on companies (passes_hard_filter, quality_score desc) where passes_hard_filter = true;

create table ingestion_runs
(
    id            SERIAL primary key,
    quarter       TEXT        not null,
    company_count INT         not null,
    ingested_at   TIMESTAMPTZ not null default now()
);
