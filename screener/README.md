# screener

A Go service that ingests SEC EDGAR's quarterly bulk financial statement data
sets, reduces ~5,700 reporting companies down to a shortlist of
value-investing candidates using price-independent hard filters, and exposes
that shortlist over REST for `core` to enrich with live prices and rank.

> This project is part of a larger educational fametrics monorepo. `core`
> (Java/Spring) handles price-dependent valuation and final ranking; this
> service only ever reasons about what's in a company's own filings.

## Features

- Downloads and stays in sync with the newest 4 quarterly SEC "Financial
  Statement Data Sets" zips (a rolling window, since a single quarter's data
  set only captures the subset of companies whose fiscal year end causes them
  to file in that specific window)
- Reduces each company to its single most recent 10-K, preferring the newest
  fiscal period and falling back to the most recently filed on a tie
  (a refiling under a new accession number)
- Extracts a fixed set of XBRL tags per company from `num.txt`, with fallback
  tags for revenue/capex/shares outstanding, filtered to clean consolidated
  (non-subsidiary, non-segment-broken-out) annual/point-in-time values
- Derives EPS, BVPS, EPS growth rate, ROE, and net margin — all price-independent
- Applies a hard filter (positive equity, two years of positive free cash
  flow, debt-to-equity under 1, ROE over 10%) and a 0–100 quality score
  (percentile rank on ROE and net margin, computed only across companies
  that passed the hard filter)
- Persists every candidate (passing or not) to Postgres and skips the entire
  extraction/scoring pipeline on a run where nothing new has actually
  appeared in SEC's data, rather than redoing several seconds of work for
  nothing
- `GET /candidates?limit=N` — what `core` calls to fetch the shortlist

## Tech stack

| Layer          | Technology                                  |
|----------------|----------------------------------------------|
| Language       | Go                                            |
| HTTP           | Standard library (`net/http`, Go 1.22+ routing) |
| Persistence    | PostgreSQL via [pgx](https://github.com/jackc/pgx) + [sqlc](https://sqlc.dev) (SQL-first, typed code generated from `db/schema.sql` + `db/queries.sql`) |
| Testing        | Standard `testing` package; DB-dependent tests use [testcontainers-go](https://golang.testcontainers.org) against a real Postgres in Docker |
| API docs       | Hand-maintained OpenAPI 3 spec at [`docs/openapi.yaml`](docs/openapi.yaml) |

## Getting started

### Prerequisites

- Go (see `go.mod` for the exact version)
- Docker (for the local Postgres via `docker-compose.yml`, and for the
  testcontainers-backed test suite)
- A `SEC_USER_AGENT_EMAIL` — SEC requires a contact email in the User-Agent
  header on every request to their site; requests without one are rejected

### Configure

```bash
cp .env.example .env
# fill in SEC_USER_AGENT_EMAIL and, if you want non-default DB credentials, DB_*
```

### Run the database

```bash
docker compose up -d
```

This starts Postgres and applies `db/schema.sql` on first boot (it's mounted
into `docker-entrypoint-initdb.d`). There are no migrations yet — the schema
is currently "create fresh" only; changing it on an existing database means
hand-writing the `ALTER TABLE` yourself.

### Run the app

```bash
go run .
```

On startup it syncs the latest SEC zips, skips the extraction/scoring
pipeline entirely if nothing's changed since the last successful run
(tracked in `ingestion_runs`), otherwise runs the full pipeline and persists
the results, then serves on `http://localhost:8081`.

### Run the tests

```bash
go test ./...
```

The `finance` package's tests are pure — no Docker, no network, nothing
external; that's deliberate, it's the package that does all the actual SEC
data parsing and computation. The root package's tests exercise the
Postgres-dependent code (`persist.go`, the `db` queries) against a real,
disposable Postgres container via testcontainers-go — Docker must be running
for those, and the first run will pull the `postgres:17` image.

## API

Full schema in [`docs/openapi.yaml`](docs/openapi.yaml). Two endpoints:

| Endpoint                | Description                                          |
|--------------------------|-------------------------------------------------------|
| `GET /health`            | Liveness check                                       |
| `GET /candidates?limit=N`| Companies passing the hard filter, ordered by quality_score descending, capped at `limit` (default 200) |

```bash
curl "http://localhost:8081/candidates?limit=5"
```

`/candidates` always queries the current state of the database fresh — no
caching or change-detection on this side; callers should do the same on
theirs rather than rely on this response staying valid.

## Project structure

```
finance/    # the pipeline: SEC data sync, parsing, fundamentals extraction,
            # metrics, hard filter, quality score — zero DB dependency,
            # fully unit-testable on its own
db/         # schema.sql + queries.sql (hand-written) and the sqlc-generated
            # Go code alongside them; connect.go is the one hand-written file
            # in this package
main.go              # wiring: sync → skip-or-recompute → persist → serve
persist.go            # translates finance.Candidate into db query params
candidates_handler.go # GET /candidates
docs/openapi.yaml     # API contract
```

## License

No license has been specified for this project yet.
