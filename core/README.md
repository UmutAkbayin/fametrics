# fa-metrics

A REST API for computing common stock valuation metrics — P/E, P/B, P/S, PEG, D/E,
ROE, Graham Number, and Peter Lynch Fair Value — from raw financial inputs.

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
- Request validation (`jakarta.validation`) with structured 400 responses
- Live, always-in-sync API documentation via springdoc-openapi / Swagger UI
- CORS enabled for local frontend development (React on `:3000`, Angular on `:4200`)

## Tech stack

| Layer          | Technology                          |
|----------------|--------------------------------------|
| Language       | Java 25                              |
| Framework      | Spring Boot 4 (Spring MVC)           |
| Persistence    | Spring Data JPA / Hibernate, PostgreSQL |
| Validation     | Jakarta Bean Validation               |
| API docs       | springdoc-openapi (OpenAPI 3 / Swagger UI) |
| Build          | Gradle                               |
| Testing        | JUnit 5, Mockito, AssertJ            |

## Getting started

### Prerequisites

- Java 25
- A running PostgreSQL instance reachable at `localhost:5432` with database
  `fa-metrics` and credentials `postgres` / `postgres` (see
  `src/main/resources/application.properties` to change these)

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

### Endpoints

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

See [`docs/openapi.yaml`](docs/openapi.yaml) for the full request/response schemas.

## Project structure

```
src/main/java/dev/akbayin/fametrics/
├── config/       # CORS and OpenAPI configuration
├── controller/   # REST controllers — thin, delegate to services
├── domain/       # MetricAssessor per metric: calculation + benchmark-based evaluation
├── dto/          # Request/response records
├── exception/    # Global exception handling
└── service/      # Routes requests to the matching MetricAssessor, assembles responses
```

## License

No license has been specified for this project yet.