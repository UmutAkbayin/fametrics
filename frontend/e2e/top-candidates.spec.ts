import { test, expect, Locator } from '@playwright/test';

const mockCandidates = [
  {
    cik: 320193,
    name: 'Apple Inc.',
    ticker: 'AAPL',
    periodEnd: '2024-09-28',
    price: 220.5,
    qualityScore: 92.4,
    valueScore: {
      composite: 75.0,
      peTtmPercentile: 70.0,
      pbRatioPercentile: 65.0,
      psRatioPercentile: 72.0,
      pegRatioPercentile: 80.0,
      discountToFairValuePercentile: 88.0
    },
    finalScore: 83.7,
    metrics: [
      {
        metric: 'PE_TTM',
        value: 28.5,
        assessment: { rating: 'NEUTRAL', label: 'Fair P/E' },
        benchmark: { lowerBound: 15, upperBound: 25 },
        description: 'Price-to-Earnings ratio',
        interpretation: 'Currently trading at a fair P/E multiple relative to historic market averages.'
      },
      {
        metric: 'ROE',
        value: 0.15,
        assessment: { rating: 'FAVORABLE', label: 'High ROE' },
        benchmark: { lowerBound: 0.1, upperBound: null },
        description: 'Return on Equity',
        interpretation: 'Strong profitability and return on equity.'
      }
    ]
  },
  {
    cik: 789019,
    name: 'Microsoft Corporation',
    ticker: 'MSFT',
    periodEnd: '2024-06-30',
    price: 430.0,
    qualityScore: 95.0,
    valueScore: {
      composite: 70.0,
      peTtmPercentile: 60.0,
      pbRatioPercentile: 55.0,
      psRatioPercentile: 65.0,
      pegRatioPercentile: 82.0,
      discountToFairValuePercentile: 88.0
    },
    finalScore: 82.5,
    metrics: [
      {
        metric: 'PE_TTM',
        value: 34.2,
        assessment: { rating: 'UNFAVORABLE', label: 'High P/E' },
        benchmark: { lowerBound: 15, upperBound: 25 },
        description: 'Price-to-Earnings ratio'
      }
    ]
  }
];

async function openTopCandidates(page: import('@playwright/test').Page): Promise<Locator> {
  await page.goto('/');
  await page.getByRole('tab', { name: 'Top Candidates' }).click();
  return page.locator('app-top-candidates');
}

test.describe('Top Candidates flow', () => {
  test('loads the Top 20 ranked candidates table', async ({ page }) => {
    await page.route('**/api/candidates/top*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCandidates)
      });
    });

    const candidatesView = await openTopCandidates(page);

    await expect(candidatesView.locator('.page-title')).toContainText('Top 20 Value Candidates');
    await expect(candidatesView.locator('.candidate-row')).toHaveCount(2);

    const firstRow = candidatesView.locator('.candidate-row').first();
    await expect(firstRow.locator('.rank-badge')).toContainText('#1');
    await expect(firstRow.locator('.ticker-badge')).toContainText('AAPL');
    await expect(firstRow.locator('.company-name')).toContainText('Apple Inc.');
    await expect(firstRow.locator('.price-value')).toContainText('$220.50');
    await expect(firstRow.locator('.final-pill')).toContainText('83.7');
  });

  test('filters candidates via search input', async ({ page }) => {
    await page.route('**/api/candidates/top*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCandidates)
      });
    });

    const candidatesView = await openTopCandidates(page);
    await expect(candidatesView.locator('.candidate-row')).toHaveCount(2);

    await candidatesView.getByLabel('Filter candidates').fill('MSFT');
    await expect(candidatesView.locator('.candidate-row')).toHaveCount(1);
    await expect(candidatesView.locator('.candidate-row').first().locator('.ticker-badge')).toContainText('MSFT');

    await candidatesView.getByLabel('Clear filter').click();
    await expect(candidatesView.locator('.candidate-row')).toHaveCount(2);
  });

  test('inspects a candidate detail and displays score breakdown and metric cards', async ({ page }) => {
    await page.route('**/api/candidates/top*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCandidates)
      });
    });

    const candidatesView = await openTopCandidates(page);

    // Click "Inspect" on the AAPL row
    await candidatesView.locator('.candidate-row').first().getByRole('button', { name: 'Inspect' }).click();

    const detailSection = candidatesView.locator('.detail-section');
    await expect(detailSection).toBeVisible();
    await expect(detailSection.locator('.candidate-name')).toContainText('Apple Inc.');
    await expect(detailSection.locator('.ticker-large')).toContainText('AAPL');

    // Score Triad
    await expect(detailSection.locator('.final-card .score-number')).toContainText('83.70');
    await expect(detailSection.locator('.quality-card .score-number')).toContainText('92.40');
    await expect(detailSection.locator('.value-card .score-number')).toContainText('75.00');

    // Metric Cards (reused MetricCardComponent)
    const peCard = detailSection.locator('app-metric-card').filter({ hasText: 'P/E (TTM)' });
    await expect(peCard.locator('.value-display')).toContainText('28.5');
    await expect(peCard.locator('.gauge-label')).toContainText('Fair P/E');

    const roeCard = detailSection.locator('app-metric-card').filter({ hasText: 'ROE' });
    await expect(roeCard.locator('.value-display')).toContainText('15.00 %');
  });

  test('opening a candidate in workspace transfers data and switches tab', async ({ page }) => {
    await page.route('**/api/candidates/top*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCandidates)
      });
    });

    const candidatesView = await openTopCandidates(page);
    await candidatesView.locator('.candidate-row').first().click();

    await candidatesView.getByRole('button', { name: 'Open in Workspace' }).click();

    // Verifies it navigated back to tab 0 (Valuation Workspace)
    const workspace = page.locator('app-metric-dashboard');
    await expect(workspace).toBeVisible();
    await expect(workspace.getByLabel('Share Price ($)')).toHaveValue('220.5');
  });

  test('displays an error alert when upstream service returns 503', async ({ page }) => {
    await page.route('**/api/candidates/top*', async (route) => {
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Upstream screener service is unavailable.' })
      });
    });

    const candidatesView = await openTopCandidates(page);

    await expect(candidatesView.locator('.error-banner')).toBeVisible();
    await expect(candidatesView.locator('.error-message')).toContainText('Upstream screener service is unavailable');
    await expect(candidatesView.locator('.retry-btn')).toBeVisible();
  });
});
