import { test, expect } from '@playwright/test';

test.describe('Theme toggle', () => {
  test('toggling the theme updates the DOM and persists across a reload', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('html')).not.toHaveClass(/light-mode/);

    await page.getByRole('button', { name: 'Toggle dark/light theme' }).click();

    await expect(page.locator('html')).toHaveClass(/light-mode/);
    await expect.poll(() => page.evaluate(() => localStorage.getItem('theme'))).toBe('light');

    await page.reload();

    await expect(page.locator('html')).toHaveClass(/light-mode/);
  });
});
