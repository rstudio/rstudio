import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import type { AxeResults, Result } from 'axe-core';
import { runAxe } from '@utils/a11y';

/**
 * Self-test for the axe rule set in utils/a11y.ts, against a synthetic page
 * rather than RStudio. Run it after any axe bump: the pins are exact, so a
 * rule-set change can only arrive that way. The control scan proves the page
 * really triggers each rule, so the "not reported" checks can't pass vacuously.
 */

// Each element below triggers one rule. The page has no landmarks, so all of
// its content also triggers `region` (best-practice).
const PAGE = `<!doctype html>
<html lang="en">
<head><title>axe scan-config self-test</title></head>
<body>
  <!-- image-alt (wcag2a) -->
  <img src="data:image/gif;base64,R0lGODlhAQABAAAAACw=" width="20" height="20">
  <!-- label-content-name-mismatch (wcag21a, experimental) -->
  <button aria-label="Close dialog">Submit form</button>
  <!-- focus-order-semantics (best-practice, experimental) -->
  <div tabindex="0">Focusable without a role</div>
  <!-- target-size (wcag22aa): 10px targets 2px apart. Kept from overlapping,
       which axe tends to report as incomplete instead. -->
  <button aria-label="First" style="position:absolute;left:20px;top:200px;width:10px;height:10px;padding:0;border:0"></button>
  <button aria-label="Second" style="position:absolute;left:32px;top:200px;width:10px;height:10px;padding:0;border:0"></button>
</body>
</html>`;

const ids = (list: Result[]) => list.map((r) => r.id);
const reported = (r: AxeResults) => [...ids(r.violations), ...ids(r.incomplete)];

test('axe scan configuration selects WCAG 2.2 A/AA rules', { tag: ['@a11y'] }, async ({ page }) => {
  await page.setContent(PAGE);

  await test.step('control: the page triggers every rule checked below', async () => {
    const control = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag22aa', 'best-practice', 'experimental'])
      .analyze();
    expect(ids(control.violations)).toEqual(expect.arrayContaining([
      'image-alt', 'target-size', 'region', 'label-content-name-mismatch', 'focus-order-semantics',
    ]));
  });

  await test.step('default: Level A and WCAG 2.2 in; best-practice and experimental out', async () => {
    const results = await runAxe(page, { experimental: false });
    expect(ids(results.violations)).toEqual(expect.arrayContaining(['image-alt', 'target-size']));
    for (const id of ['region', 'label-content-name-mismatch', 'focus-order-semantics'])
      expect(reported(results), id).not.toContain(id);
  });

  await test.step('experimental: adds experimental WCAG rules only', async () => {
    const results = await runAxe(page, { experimental: true });
    expect(ids(results.violations)).toContain('label-content-name-mismatch');
    for (const id of ['focus-order-semantics', 'region'])
      expect(reported(results), id).not.toContain(id);
  });
});
