import { test, expect } from '@playwright/test';

test.describe('Valuation Workspace (dashboard)', () => {
  test('loading the sample data set calculates every metric against the real backend', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('button', { name: 'Load Sample' }).click();

    const peCard = page.locator('app-metric-card').filter({ hasText: 'P/E (TTM)' });
    await expect(peCard.locator('.value-display')).toContainText('20');
    await expect(peCard.locator('.gauge-label')).toContainText('Fair P/E');

    const roeCard = page.locator('app-metric-card').filter({ hasText: 'ROE' });
    await expect(roeCard.locator('.value-display')).toContainText('15.00 %');

    const grahamCard = page.locator('app-metric-card').filter({ hasText: 'Graham #' });
    await expect(grahamCard.locator('.value-display')).toContainText('$ 87.14');
  });

  test('an out-of-range input blocks submission client-side, without calling the backend', async ({ page }) => {
    await page.goto('/');

    await page.getByLabel('Total Liabilities ($)').fill('-5');
    await page.getByRole('button', { name: 'Calculate Metrics' }).click();

    await expect(page.locator('.error-alert')).toContainText(
      'Please fix the validation errors in the inputs panel first.',
    );
    await expect(page.locator('app-metric-card').first()).toContainText('Awaiting Inputs');
  });
});
