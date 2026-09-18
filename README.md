# fametrics

A polyglot, three-service pipeline that turns SEC EDGAR's raw bulk filings into
a ranked, explainable shortlist of value-investing candidates: a Go service
ingests and screens on fundamentals alone, a Java/Spring service enriches the
survivors with live prices and a full valuation-metric breakdown, and an
Angular frontend presents the result — and also doubles as a standalone
metrics sandbox and an educational metric explorer.

> This is an educational portfolio project. Valuation logic, benchmarks, and
> weightings throughout are intentionally simplified starting points, not
> researched investment advice — see each service's own README for the
> specific judgment calls made and why.

## Architecture

```
┌──────────────┐  bulk 10-K data   ┌──────────────┐
│  SEC EDGAR   │ ────────────────► │              │
└──────────────┘                   │   screener   │  Go · Postgres
                                    │              │  ingest → hard-filter →
                                    │              │  quality_score
                                    └──────┬───────┘
                                           │ GET /candidates
                                           ▼
┌──────────────┐   ticker lookup   ┌──────────────┐
│  SEC (free   │ ◄──────────────── │              │
│  ticker file)│                   │     core     │  Java 25 · Spring Boot 4
└──────────────┘                   │              │  (stateless, no DB yet)
                                    │              │  price + rank + the
┌──────────────┐   live price      │              │  existing valuation engine
│     FMP      │ ◄──────────────── │              │
└──────────────┘                   └──────┬───────┘
                                           │ GET /api/candidates/top
                                           │ POST /api/metrics/...
                                           ▼
                                    ┌──────────────┐
                                    │   frontend   │  Angular 22
                                    │              │  ranked table, deep-dive,
                                    │              │  standalone metric sandbox
                                    └──────────────┘
```

Each arrow is a real network call, not a shared library or a shared database —
the three services only ever talk to each other over HTTP. `screener` never
touches price; `core` never re-derives fundamentals the screener already
computed; the existing valuation engine in `core` is called in-process by the
ranking layer, never re-implemented.

## Components

| Service | Stack | Role | Docs |
|---|---|---|---|
| [`screener`](screener/) | Go, PostgreSQL | Downloads SEC's quarterly bulk financial statement data sets, reduces each company to its latest 10-K, extracts fundamentals from raw XBRL, and applies price-independent hard filters (positive equity, 2 years of positive FCF, D/E < 1, ROE > 10%) plus a fundamentals-only `quality_score`. Exposes the survivors over REST. | [README](screener/README.md) · [OpenAPI](screener/docs/openapi.yaml) |
| [`core`](core/) | Java 25, Spring Boot 4 | A standalone valuation-metrics engine (P/E, P/B, P/S, PEG, D/E, ROE, Graham Number, Lynch Fair Value) callable directly per request, *plus* an orchestration layer that pulls `screener`'s candidates, resolves and prices them via FMP, runs them through that same engine in-process, computes a price-dependent Value Score, and returns a combined ranked Top N. Entirely stateless — no database of its own yet. | [README](core/README.md) · [OpenAPI](core/docs/openapi.yaml) |
| [`frontend`](frontend/) | Angular 22 (zoneless) | Three workspaces: a ranked Top 20 candidate table with per-candidate deep dives, an interactive sandbox for the raw metrics engine, and an educational metric-by-metric explorer. | [README](frontend/README.md) |

## Getting started

`screener` is the only service with its own database; `core` and `frontend`
are stateless. The frontend's Top Candidates view needs the full chain
(`screener` → `core` → `frontend`) running to show real data. Suggested order:

1. **`screener`** — needs its own Postgres and a `SEC_USER_AGENT_EMAIL` (SEC
   requires a real contact address in every request's User-Agent, or it
   returns 403):
   ```bash
   cd screener && docker compose up -d && go run .
   ```
   See [`screener/README.md`](screener/README.md) for the full env var list.
2. **`core`** — needs `SEC_USER_AGENT_EMAIL` (same requirement, its own SEC
   ticker-lookup client) and `FMP_API_KEY` (a free Financial Modeling Prep
   key is enough):
   ```bash
   cd core && ./gradlew bootRun
   ```
   See [`core/README.md`](core/README.md) for how those env vars need to
   reach the process.
3. **`frontend`**:
   ```bash
   cd frontend && npm install && npm start
   ```
   Then open `http://localhost:4200`. See [`frontend/README.md`](frontend/README.md).

`.env` support differs by language: the Go `screener` loads a `.env` file
itself on startup (`godotenv`), so `cp .env.example .env` there is enough.
Spring Boot has no built-in `.env` support, so `core`'s env vars need to
actually land in the process environment some other way — a real `export`,
an IDE run configuration, or a plugin like IntelliJ's EnvFile reading a local
`.env` — not just exist in a file sitting in the directory.

`core` degrades gracefully rather than failing outright if `screener`, SEC,
or FMP are unreachable when it starts or while running — scheduled background
refreshes retry automatically, and `/api/candidates/top` returns a `503`
(not a raw `500`) if the screener itself can't be reached live. `/api/metrics/...`
never depends on any of that — it's pure per-request calculation.

## License

MIT — see [`LICENSE`](LICENSE).
