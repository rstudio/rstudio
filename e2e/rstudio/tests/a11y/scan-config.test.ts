import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import type { AxeResults, Result } from 'axe-core';
import { runAxe, type RuleSet } from '@utils/a11y';

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
  <!-- color-contrast-enhanced (wcag2aaa): #767676 on white is 4.54:1, above
       AA's 4.5:1 and below AAA's 7:1. -->
  <p style="color:#767676;background:#fff">Low-contrast text</p>
  <!-- target-size (wcag22aa): 10px targets 2px apart. Kept from overlapping,
       which axe tends to report as incomplete instead. -->
  <button aria-label="First" style="position:absolute;left:20px;top:200px;width:10px;height:10px;padding:0;border:0"></button>
  <button aria-label="Second" style="position:absolute;left:32px;top:200px;width:10px;height:10px;padding:0;border:0"></button>
</body>
</html>`;

const ids = (list: Result[]) => list.map((r) => r.id);
const reported = (r: AxeResults) => [...ids(r.violations), ...ids(r.incomplete)];
const DEFAULT: RuleSet = { level: 'AA', version: '2.2', bestPractices: false, experimental: false };

test('axe scan configuration selects rules by WCAG level, version and extra groups', { tag: ['@a11y'] }, async ({ page }) => {
  await page.setContent(PAGE);

  await test.step('control: the page triggers every rule checked below', async () => {
    const control = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aaa', 'wcag22aa', 'best-practice', 'experimental'])
      .analyze();
    expect(ids(control.violations)).toEqual(expect.arrayContaining([
      'image-alt', 'color-contrast-enhanced', 'target-size', 'region', 'label-content-name-mismatch',
      'focus-order-semantics',
    ]));
  });

  await test.step('default: WCAG 2.2 A/AA in; AAA, best-practice and experimental out', async () => {
    const results = await runAxe(page, DEFAULT);
    expect(ids(results.violations)).toEqual(expect.arrayContaining(['image-alt', 'target-size']));
    for (const id of ['color-contrast-enhanced', 'region', 'label-content-name-mismatch', 'focus-order-semantics'])
      expect(reported(results), id).not.toContain(id);
  });

  await test.step('level A: drops AA rules', async () => {
    const results = await runAxe(page, { ...DEFAULT, level: 'A' });
    expect(ids(results.violations)).toContain('image-alt');
    expect(reported(results)).not.toContain('target-size');
  });

  await test.step('level AAA: adds AAA rules', async () => {
    const results = await runAxe(page, { ...DEFAULT, level: 'AAA' });
    expect(ids(results.violations)).toEqual(
      expect.arrayContaining(['image-alt', 'target-size', 'color-contrast-enhanced']));
  });

  await test.step('WCAG 2.1: drops WCAG 2.2 rules', async () => {
    const results = await runAxe(page, { ...DEFAULT, version: '2.1' });
    expect(ids(results.violations)).toContain('image-alt');
    expect(reported(results)).not.toContain('target-size');
  });

  await test.step('experimental: adds experimental WCAG rules only', async () => {
    const results = await runAxe(page, { ...DEFAULT, experimental: true });
    expect(ids(results.violations)).toContain('label-content-name-mismatch');
    for (const id of ['focus-order-semantics', 'region'])
      expect(reported(results), id).not.toContain(id);
  });

  await test.step('best practices: adds best-practice rules, not experimental ones', async () => {
    const results = await runAxe(page, { ...DEFAULT, bestPractices: true });
    expect(ids(results.violations)).toEqual(expect.arrayContaining(['image-alt', 'region']));
    for (const id of ['label-content-name-mismatch', 'focus-order-semantics'])
      expect(reported(results), id).not.toContain(id);
  });

  await test.step('best practices + experimental: adds experimental best-practice rules', async () => {
    const results = await runAxe(page, { ...DEFAULT, bestPractices: true, experimental: true });
    expect(ids(results.violations)).toEqual(expect.arrayContaining(
      ['region', 'label-content-name-mismatch', 'focus-order-semantics']));
  });
});
