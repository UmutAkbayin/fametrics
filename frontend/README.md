# FaMetrics Frontend

A modern, responsive financial valuation and screening platform built with **Angular 22** (zoneless architecture, Angular Material 3, and Glassmorphic financial terminal design).

---

## Workspace Features

FaMetrics is organized into three unified tabs designed for different stages of the investment research workflow:

### 1. Valuation Workspace
- **Interactive Financial Modeling Sandbox**: Enter raw financial variables (`sharePrice`, `eps`, `bvps`, `marketCap`, `totalRevenue`, `epsGrowthRate`, `totalLiabilities`, `totalEquity`, `netIncome`) to compute all 8 valuation metrics at once against `core`'s `/api/metrics/summary/assessment`.
- **Real-Time Validation**: Field validation for positive inputs, non-negative liabilities, and growth bounds with missing-input indicators on each card.
- **Sample Loader**: Load pre-packaged sample financial data with one click to quickly test the engine.

### 2. Top Candidates (Ranked Value Screener)
- **Automated Pipeline Discovery**: Surfaces the Top 20 value-investing candidates produced by `core`'s `/api/candidates/top` orchestration endpoint.
  - **Screener Stage (Go)**: Ingests annual SEC EDGAR 10-K filings, applies hard filters (positive equity, 2 years of positive free cash flow, D/E < 1, ROE > 10%), and scores candidates on ROE and net margin to produce a 0–100 `qualityScore`.
  - **Pricing & Valuation Stage (Core Java)**: Resolves SEC CIKs to market tickers, enriches candidates with cached live prices from Financial Modeling Prep (FMP), runs each candidate through the in-process valuation engine, and calculates 5 price-dependent percentile rankings (P/E, P/B, P/S, PEG, Discount to Fair Value) averaged into a `valueScore.composite`.
  - **Ranking**: Combines Quality Score (50%) and Value Score Composite (50%) into a sorted `finalScore`.
- **Master Table**: Displays rank (#1–#20, with medal highlights for the top 3), ticker, company name, CIK, market price, fiscal year end date, quality score, value score, and final score.
- **Interactive Filtering & Sorting**: Filter instantly by ticker symbol, company name, or CIK. Sort dynamically by Final Score, Quality Score, Value Score, Ticker, or Price.
- **Candidate Deep Dive**: Selecting any candidate displays:
  - **Score Triad**: Visual breakdown of Final Score, Screener Quality Score, and Core Value Score.
  - **5-Factor Percentile Gauges**: Individual percentile ranks for P/E (TTM), P/B, P/S, PEG, and Fair Value Discount across the candidate pool.
  - **Valuation Metrics Assessment**: Reuses the 8 valuation metric cards to display calculated metric values, benchmark ranges, favorable/neutral/unfavorable status badges, and plain-English financial interpretations.
  - **Open in Workspace**: Bridges the candidate data directly into the Valuation Workspace tab with one click for what-if sensitivity analysis.
- **Resilience**: Gracefully handles upstream service outages (e.g. HTTP 503 if the Go screener is unreachable) with informative error alerts and retry buttons.

### 3. Metrics Explorer
- **Educational Metric Laboratory**: Learn the theory, mathematical formulation, and benchmark thresholds of individual valuation metrics one at a time.
- **Dedicated Calculations**: Calls individual single-metric value endpoints (`/api/metrics/{path}`) to compute focused metrics with isolated inputs.

---

## Tech Stack & Architecture

- **Framework**: Angular 22 (Zoneless with `provideZonelessChangeDetection()`, Signal-based reactive state)
- **Design System**: Angular Material 3 with custom CSS tokens supporting dynamic dark/light theme switching
- **Styling**: SCSS, Glassmorphism, responsive data table and card grids
- **Testing**:
  - Unit Tests: [Vitest](https://vitest.dev/) with Angular HttpClientTesting (`ng test --watch=false`)
  - End-to-End Tests: [Playwright](https://playwright.dev/) (`npx playwright test`)

---

## Getting Started

### Development server

```bash
npm start # or ng serve
```

Navigate to `http://localhost:4200/`.

### Running Unit Tests

```bash
npm test -- --watch=false
```

### Running End-to-End Tests

```bash
npx playwright test
```

To run a specific test suite or inspect results:

```bash
npx playwright test e2e/top-candidates.spec.ts
npx playwright test e2e/theme-toggle.spec.ts
```

### Building for Production

```bash
npm run build # or ng build
```

The production bundle will be built in the `dist/fa-metrics-frontend` directory.

---

## License

MIT — see the [repository root `LICENSE`](../LICENSE).
