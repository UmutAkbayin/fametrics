# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- `npm start` / `ng serve` — run the dev server at `http://localhost:4200` (auto-reloads on source changes).
- `ng build` — production build, output to `dist/`. `ng build --configuration development` (or `npm run watch`) for an unoptimized build with source maps.
- `ng test` — intended to run unit tests via Vitest per the Angular CLI defaults. Note: there are currently no `*.spec.ts` files and no test target configured in `angular.json`, so this is not yet wired up — check both before assuming test tooling works.
- `ng generate component path/to/name` — scaffold a new standalone component (schematics default to SCSS, standalone, and no spec file — see `angular.json`).
- No lint script is defined in `package.json`; Prettier config exists (`.prettierrc`, 100 char width, single quotes, Angular parser for `.html`).

## Architecture

This is a standalone-components Angular 22 app (no `NgModule`s). Bootstrap goes through `src/main.ts` → `bootstrapApplication(App, appConfig)`, with app-wide providers (`provideZonelessChangeDetection`, HttpClient, async animations, global error listeners) registered in `src/app/app.config.ts`.

The app is **zoneless** — there is no `zone.js` dependency, so Angular has no automatic way to detect that an async callback (e.g. an HTTP `subscribe`) has updated component state. Component state that changes outside a template-bound event (HTTP responses, timers, etc.) must live in a `signal()` (or `computed()`), never a plain class field — a plain-field mutation from an async callback silently never repaints. This is why `result`/`loading`/`errorMessage`/`results` on the dashboard/explorer/card components are signals, read in templates as `foo()`, and why `MetricCardComponent`'s `@Input()`s are `input()`/`input.required()` signals rather than decorators.

Templates use Angular's native control-flow syntax (`@if`, `@for`) throughout, not `*ngIf`/`*ngFor`.

### Component tree

`App` (`app.component.ts`) composes three top-level standalone components, each self-contained with its own `.ts`/`.html`/`.scss`:

- **`HeaderComponent`** — toolbar and dark/light theme toggle. Theme state is persisted to `localStorage` and applied by toggling a `light-mode` class on `<html>` (see `src/styles/_theme.scss` for the corresponding SCSS variables/overrides).
- **`MetricDashboardComponent`** — the main "enter all your data, get every metric at once" screen. Owns a single reactive `FormGroup` covering all possible inputs (`sharePrice`, `eps`, `bvps`, `marketCap`, `totalRevenue`, `epsGrowthRate`, `totalLiabilities`, `totalEquity`, `netIncome`), and a local `metrics: MetricDef[]` array that declares each supported metric's id/title/formula/required fields. On `calculate()`, it builds a `SummaryRequest` and posts it via `StockValuationService`, then renders one `MetricCardComponent` per metric (`@for` over `metrics`), passing it the matching slice of the `SummaryResponse` plus per-card missing-input state (`getMissingInputs`).
- **`MetricExplorerComponent`** — an educational "playground" for one metric at a time. `metricDetails` is a large local dictionary keyed by metric id holding the formula, LaTeX-ish `mathDisplay` string, prose description, per-metric input definitions/validators, interpretation guide, and benchmark thresholds. Selecting a metric (`selectMetric`) rebuilds a fresh `FormGroup` from that metric's `inputs`. `calculate()` looks up the metric's endpoint path in the local `metricPaths` map, builds a `SummaryRequest` from the form values via `buildSummaryRequest()`, and posts it to that metric's single-value endpoint via `StockValuationService.calculateMetricValue()`.
- **`MetricCardComponent`** — presentational, `@Input()`-driven card used by the dashboard. Derives a `CardDisplayState` (label/status color/progress percentage/hint) from a `MetricResponse`'s `assessment.rating` (`FAVORABLE` / `NEUTRAL` / `UNFAVORABLE` / `NOT_MEANINGFUL`), or shows a "missing inputs" state when no result is available yet.

### Data layer

`src/app/services/stock-valuation.service.ts` is the sole service and the single source of truth for API types, which are hand-mirrored from an OpenAPI 3.0.3 backend schema (see the block comments in that file): `MetricType`, `Rating`, `MarketData`, `FundamentalData`, `CapitalStructure`, `SummaryRequest`/`SummaryResponse`, `Assessment`, `Benchmark`, `MetricResponse`. `apiUrl` is hardcoded to `http://localhost:8080/api/metrics`; there is no environment-based API URL configuration — changing the backend target means editing this constant directly. The backend exposes every metric two ways, both accepting the same `SummaryRequest` superset body (each endpoint just reads the sub-object(s) it needs and treats missing/invalid inputs as uncomputable rather than rejecting the request):
  - **Per-metric value-only endpoints** — `POST {apiUrl}/{path}` (`pe-ttm`, `pb`, `ps`, `peg`, `de`, `roe`, `graham`, `lynch`) return a bare `number`. `MetricExplorerComponent` uses these via `calculateMetricValue(path, req)`, since its focused calculator only needs one metric's value.
  - **The summary/assessment endpoint** — `calculateSummary()` → `POST {apiUrl}/summary/assessment` runs every metric at once and returns a full `SummaryResponse` (value + rating + benchmark per metric, omitting any that couldn't be computed). `MetricDashboardComponent` uses this since it renders all eight metrics from one shared input form.

  Any new metric or field must be added consistently across this service's types, `MetricDashboardComponent.metrics`, and `MetricExplorerComponent.metricDetails`/`metricPaths`, since none of these are generated from a shared schema.
