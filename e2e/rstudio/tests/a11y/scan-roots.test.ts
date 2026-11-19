import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { assertRootsCoverWindow } from '@utils/a11y';

/**
 * Self-test for assertRootsCoverWindow in utils/a11y.ts, against a synthetic
 * page shaped like RStudio's: html with overflow: hidden (as GWT's
 * Window.enableScrolling(false) sets it) over an absolutely positioned layout.
 * Each step also scans for color-contrast, to show the guard fails exactly
 * when axe would skip the low-contrast text.
 */

const pageHtml = (rootStyle: string) => `<!doctype html>
<html lang="en" style="overflow: hidden; ${rootStyle}">
<head><title>axe root-size self-test</title></head>
<body style="margin: 0; ${rootStyle}">
  <div style="position: absolute; inset: 0">
    <!-- #767676 on #e6e6e6 is 3.6:1, below AA's 4.5:1. -->
    <p style="color: #767676; background: #e6e6e6">Low-contrast text</p>
  </div>
</body>
</html>`;

const contrastViolations = async (page: Page) =>
  (await new AxeBuilder({ page }).withRules(['color-contrast']).analyze()).violations;

test('scan guard rejects root elements that hide the page from axe', { tag: ['@a11y'] },
  async ({ page }) => {
    await test.step('0px html, overflow: hidden: axe skips the text; guard throws', async () => {
      await page.setContent(pageHtml(''));
      expect(await contrastViolations(page)).toEqual([]);
      await expect(assertRootsCoverWindow(page))
        .rejects.toThrow(/<html> is \d+x0px .* with overflow: hidden/);
    });

    await test.step('html and body at 100%: axe reports the text; guard passes', async () => {
      await page.setContent(pageHtml('height: 100%;'));
      expect((await contrastViolations(page)).map((v) => v.id)).toEqual(['color-contrast']);
      await assertRootsCoverWindow(page);
    });

    await test.step('a root without overflow: hidden never clips, whatever its size', async () => {
      await page.setContent('<!doctype html><html lang="en"><body><p>Short page</p></body></html>');
      await assertRootsCoverWindow(page);
    });
  });
