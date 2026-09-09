import { test, expect, Locator } from '@playwright/test';

async function openExplorer(page: import('@playwright/test').Page): Promise<Locator> {
  await page.goto('/');
  await page.getByRole('tab', { name: 'Metrics Explorer' }).click();
  return page.locator('app-metric-explorer');
}

test.describe('Metrics Explorer (focused calculator)', () => {
  test('computes P/E (TTM) via the dedicated /pe-ttm endpoint', async ({ page }) => {
    const explorer = await openExplorer(page);

    await explorer.getByLabel('Share Price ($)').fill('150');
    await explorer.getByLabel('Earnings Per Share (EPS, $)').fill('10');
    await explorer.getByRole('button', { name: 'Compute Metric' }).click();

    await expect(explorer.locator('.result-value')).toContainText('15');
    await expect(explorer.locator('.result-spinner')).toHaveCount(0);
  });

  test('switching metrics posts to that metric\'s own endpoint (/de)', async ({ page }) => {
    const explorer = await openExplorer(page);

    await explorer.getByText('Debt-to-Equity Ratio').click();
    await explorer.getByLabel('Total Liabilities ($)').fill('20000000');
    await explorer.getByLabel('Total Shareholder Equity ($)').fill('80000000');
    await explorer.getByRole('button', { name: 'Compute Metric' }).click();

    await expect(explorer.locator('.result-value')).toContainText('0.25');
  });

  test('blocks submission client-side when a required field is left empty', async ({ page }) => {
    const explorer = await openExplorer(page);

    await explorer.getByRole('button', { name: 'Compute Metric' }).click();

    await expect(explorer.locator('.result-error')).toContainText(
      'Please provide valid inputs to calculate.',
    );
  });
});
