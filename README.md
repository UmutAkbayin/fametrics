# fa-metrics

A REST API for computing common stock valuation metrics — P/E, P/B, P/S, PEG, D/E,
ROE, Graham Number, and Peter Lynch Fair Value — from raw financial inputs.

Built with Spring Boot 4 and Java 25. All calculations use `BigDecimal` with
explicit rounding, so results are deterministic and free of floating-point error.

> This project is an educational fundamental analysis application. Valuation
> metrics and benchmarks are intentionally simplified and will be refined over
> time as the project evolves.

## Features

- Eight individual valuation metric endpoints, plus a `/summary` endpoint that
  computes all of them in a single request
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

All endpoints are `POST` requests under `/api/metrics` that accept a JSON body
and return a `BigDecimal` result (or `422 Unprocessable Content` if the given
inputs can't produce a valid result).

| Endpoint                | Metric                        |
|--------------------------|-------------------------------|
| `POST /api/metrics/pe-ttm` | Price-to-Earnings (TTM)     |
| `POST /api/metrics/pb`     | Price-to-Book               |
| `POST /api/metrics/ps`     | Price-to-Sales               |
| `POST /api/metrics/peg`    | Price/Earnings-to-Growth     |
| `POST /api/metrics/de`     | Debt-to-Equity               |
| `POST /api/metrics/roe`    | Return on Equity             |
| `POST /api/metrics/graham` | Graham Number                |
| `POST /api/metrics/lynch`  | Peter Lynch Fair Value       |
| `POST /api/metrics/summary`| All of the above in one call |

Example request:

```bash
curl -X POST http://localhost:8080/api/metrics/graham \
  -H "Content-Type: application/json" \
  -d '{"eps": 3.5, "bvps": 22.0}'
```

## Project structure

```
src/main/java/dev/akbayin/fametrics/
├── config/       # CORS and OpenAPI configuration
├── controller/   # REST controllers — thin, delegate to services
├── dto/          # Request/response records
├── exception/    # Global exception handling
└── service/      # Valuation calculation logic
```

## License

No license has been specified for this project yet.