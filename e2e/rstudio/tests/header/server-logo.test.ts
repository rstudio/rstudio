import { test, expect } from '@fixtures/rstudio.fixture';

// The Server header logo is the banner landmark's only content, so it needs a
// name. When Workbench links it to its home page the image names the link, so
// its alt text must describe the destination rather than the product.
test.describe('Server header logo', { tag: ['@server_only'] }, () => {
  test('logo has an accessible name', async ({ rstudioPage: page }) => {
    const anchor = page.locator('#rstudio_rstudio_logo');
    const logo = anchor.locator('img');
    await expect(logo).toBeVisible();

    if (await anchor.getAttribute('href') === null) {
      await expect(logo).toHaveAccessibleName('RStudio');
    } else {
      await expect(page.getByRole('link', { name: 'Posit Workbench' })).toBeVisible();
    }
  });
});
