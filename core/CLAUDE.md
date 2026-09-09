# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

fa-metrics is a Spring Boot 4 REST API (Java 25) for financial/stock valuation metrics, backed by PostgreSQL via JPA/Hibernate. It is early-stage: currently one domain entity (`Company`) and one valuation calculation (Graham Number).

## Commands

- Run the app: `./gradlew bootRun` (requires PostgreSQL running, see below)
- Build: `./gradlew build`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "dev.akbayin.fametrics.service.ValuationServiceTest"`
- Run a single test method: `./gradlew test --tests "dev.akbayin.fametrics.service.ValuationServiceTest.calculateGrahamNumber_whenInputIsValid_shouldReturnValue"`
- Start local database: `docker compose up -d` (Postgres 18, exposed on `localhost:5432`, db `fa-metrics`, user/password `postgres`/`postgres`)

## Architecture

Standard layered Spring MVC structure under `dev.akbayin.fametrics`:

- `controller/` — `@RestController`s, thin, delegate directly to services. Return `Optional`-mapped `ResponseEntity` (e.g. `unprocessableContent()` when a calculation can't be performed rather than throwing).
- `service/` — business/calculation logic, annotated `@Service` with constructor injection (`@RequiredArgsConstructor`).
- `entity/` — JPA `@Entity` classes, built with Lombok (`@Builder`, `@Getter`, `@AllArgsConstructor`, `@NoArgsConstructor`). Entities currently double as request/response DTOs (e.g. `Company` is both the JPA entity and the `@RequestBody` for the valuation endpoint) — there is no separate DTO layer yet.

Financial calculations use `BigDecimal` throughout (never `double`/`float`) with explicit `MathContext`/`RoundingMode` for rounding — follow this convention for any new valuation logic.

Database schema is managed via `spring.jpa.hibernate.ddl-auto=update` (Hibernate auto-updates the schema from entities) — there are no Flyway/Liquibase migrations in this project.

## Testing conventions

- Unit tests for services use JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@InjectMocks`) and AssertJ assertions (`assertThat`).
- Parameterized tests (`@ParameterizedTest` + `@MethodSource`) are used for covering multiple invalid-input cases in one test.

## Conventions
- Git commits: Follow Conventional Commits format in English (e.g., `feat(valuation): add DCF calculation`).
- Code comments: Write in English, keep them sparse and focus on "why" not "how".