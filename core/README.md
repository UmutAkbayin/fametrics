# fa-metrics

A REST API for computing common stock valuation metrics — P/E, P/B, P/S, PEG, D/E,
ROE, Graham Number, and Peter Lynch Fair Value — from raw financial inputs, plus an
orchestration layer that turns a companion screener service's fundamentals-only
candidates into a fully-priced, ranked Top N list for a frontend to consume.

Built with Spring Boot 4 and Java 25. All calculations use `BigDecimal` with
explicit rounding, so results are deterministic and free of floating-point error.

> This project is an educational fundamental analysis application. Valuation
> metrics and benchmarks are intentionally simplified and will be refined over
> time as the project evolves.

## Features

- Eight valuation metrics, each with a raw-value endpoint and a full
  assessment endpoint (value + benchmark-based rating + interpretation),
  plus a `/summary/assessment` endpoint that returns every metric's
  assessment in one call
- **`GET /api/candidates/top`** — pulls candidates from the `screener`
  service, prices them from an FMP-backed cache, runs every candidate
  through the same valuation engine above in-process (no self HTTP calls),
  computes a price-dependent Value Score alongside the screener's own
  fundamentals-only quality_score, and returns the combined ranking
- CIK-to-ticker resolution (SEC's own free `company_tickers.json`) and FMP
  pricing are both refreshed on a schedule, not live per request — FMP's
  free tier is 250 calls/day with no batch endpoint, so pricing ~200
  candidates on every request isn't viable; prices are "as of the last
  refresh," a deliberate trade for staying on a free plan
- Resilient to upstream failures by design, not by accident: a scheduled
  refresh failure (screener, SEC, or FMP down) logs and keeps serving the
  last good cache rather than crashing; a live request-path failure (e.g.
  the screener being unreachable when `/api/candidates/top` is called)
  returns `503 Service Unavailable` with a clear message, not a raw `500`
- Request validation (`jakarta.validation`) with structured 400 responses
- Live, always-in-sync API documentation via springdoc-openapi / Swagger UI
- CORS enabled for local frontend development (React on `:3000`, Angular on `:4200`)

## Tech stack

| Layer          | Technology                          |
|----------------|--------------------------------------|
| Language       | Java 25                              |
| Framework      | Spring Boot 4 (Spring MVC)           |
| HTTP clients   | Spring `RestClient` (screener, FMP, SEC), scheduled via `@Scheduled` |
| Validation     | Jakarta Bean Validation               |
| API docs       | springdoc-openapi (OpenAPI 3 / Swagger UI) |
| Build          | Gradle                               |
| Testing        | JUnit 5, Mockito, AssertJ            |

## Getting started

### Prerequisites

- Java 25
- `SEC_USER_AGENT_EMAIL` — SEC requires a real contact email in the
  User-Agent header on every request to `company_tickers.json`, or it
  rejects the request with `403`; same requirement the `screener` (Go)
  service has, same env var name
- `FMP_API_KEY` — a Financial Modeling Prep API key (the free tier is
  enough; see the note on call budget above)
- The `screener` service running at `screener.base-url`
  (`http://localhost:8081` by default) for `/api/candidates/top` to return
  real data — the rest of the API (`/api/metrics/...`) doesn't need it

Both env vars are read as plain OS environment variables (`${SEC_USER_AGENT_EMAIL}`,
`${FMP_API_KEY}` in `application.properties`) — Spring Boot has no built-in
`.env` file support, unlike the Go screener. A `.env` file is still a
reasonable place to keep them for local dev (gitignored; this repo has one
per module) as long as *something* actually loads it into the process
environment before the JVM starts — e.g. IntelliJ's EnvFile plugin, or your
run configuration's environment variables field.

### Run the app

```bash
./gradlew bootRun
```

The API is served on `http://localhost:8080`.

### Run the tests

```bash
./gradlew test
```

## API documentation

Once the app is running, the OpenAPI spec and interactive docs are served live,
generated directly from the code:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

A static snapshot of the spec is also checked in at [`docs/openapi.yaml`](docs/openapi.yaml).

### Valuation endpoints

All endpoints are `POST` requests under `/api/metrics` that accept the same
request body shape — see below. The plain endpoint returns just the
calculated `BigDecimal` value; the `/assessment` variant returns the value
plus a benchmark-based rating and a human-readable interpretation. Both
return `422 Unprocessable Content` if the given inputs can't produce a valid
result for that metric.

| Metric                    | Value endpoint                | Assessment endpoint                      |
|----------------------------|--------------------------------|-------------------------------------------|
| Price-to-Earnings (TTM)    | `POST /api/metrics/pe-ttm`    | `POST /api/metrics/pe-ttm/assessment`    |
| Price-to-Book              | `POST /api/metrics/pb`        | `POST /api/metrics/pb/assessment`        |
| Price-to-Sales             | `POST /api/metrics/ps`        | `POST /api/metrics/ps/assessment`        |
| Price/Earnings-to-Growth   | `POST /api/metrics/peg`       | `POST /api/metrics/peg/assessment`       |
| Debt-to-Equity             | `POST /api/metrics/de`        | `POST /api/metrics/de/assessment`        |
| Return on Equity           | `POST /api/metrics/roe`       | `POST /api/metrics/roe/assessment`       |
| Graham Number              | `POST /api/metrics/graham`    | `POST /api/metrics/graham/assessment`    |
| Peter Lynch Fair Value     | `POST /api/metrics/lynch`     | `POST /api/metrics/lynch/assessment`     |

`POST /api/metrics/summary/assessment` runs every metric's assessment against
the same request body and returns the ones that could be computed — never
`422`; metrics that can't be computed from the given inputs are simply
omitted.

Every endpoint takes the same request body — a superset of inputs grouped by
where they come from, with nothing required at the schema level:

```json
{
  "marketData": { "sharePrice": 40.38, "eps": 2.93, "bvps": 47.65 },
  "fundamentalData": { "marketCap": 50, "totalRevenue": 100, "epsGrowthRate": 0.15 },
  "capitalStructure": { "totalLiabilities": 150000, "totalEquity": 100000, "netIncome": 15000 }
}
```

Each metric only reads the sub-object(s) it needs; the rest can be omitted.
Example requests:

```bash
curl -X POST http://localhost:8080/api/metrics/graham \
  -H "Content-Type: application/json" \
  -d '{"marketData": {"eps": 3.5, "bvps": 22.0}}'

curl -X POST http://localhost:8080/api/metrics/graham/assessment \
  -H "Content-Type: application/json" \
  -d '{"marketData": {"sharePrice": 60.0, "eps": 3.5, "bvps": 22.0}}'
```

### Candidates endpoint

```bash
curl http://localhost:8080/api/candidates/top?limit=20
```

Returns the Top N candidates (default 20), each with its `cik`/`name`/`ticker`,
the cached `price` it was ranked against, the screener's `qualityScore`, core's
own `valueScore` (and its percentile components), the combined `finalScore`
used to sort, and the full per-metric detail (`metrics`) from the Valuation
engine — the same shape `/summary/assessment` returns.

A candidate only appears if **both** `qualityScore` and `valueScore.composite`
are present — one missing an entire factor (e.g. no ticker could be resolved
for its CIK, so no price, so no price-dependent metrics at all) is left out
rather than scored on whichever single factor it has. `503` means the
screener itself was unreachable for this request, not that nothing qualified.

## Project structure

```
src/main/java/dev/akbayin/fametrics/
├── config/       # CORS and OpenAPI configuration
├── controller/   # REST controllers — thin, delegate to services
├── domain/       # MetricAssessor per metric: calculation + benchmark-based evaluation
├── dto/          # Request/response records (including TopCandidateResponse)
├── exception/    # Global exception handling — validation errors (400) and
│                 # upstream service failures (503)
├── service/      # Routes requests to the matching MetricAssessor, assembles responses
├── screener/     # Client + DTO for the screener service's GET /candidates
├── sec/          # SEC company_tickers.json client + cached, scheduled CIK-to-ticker lookup
├── fmp/          # FMP client + DTO for the /stable/profile endpoint
├── pricing/      # Cached, scheduled price snapshots keyed by CIK (PriceCache)
└── ranking/      # The orchestration pipeline: CandidateAssessor (runs the
                  # existing engine per candidate) → ValueScoreCalculator
                  # (price-dependent percentile ranking) → FinalScoreCalculator
                  # (combines with quality_score, sorts, takes top N) →
                  # TopCandidatesService (ties it together) → TopCandidateMapper
```

## License

MIT — see the [repository root `LICENSE`](../LICENSE).
