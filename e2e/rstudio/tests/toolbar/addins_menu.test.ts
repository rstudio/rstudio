// Main toolbar > Addins: keyboard navigation and screen reader semantics (#19552).
//
// The search box sat inside the menu and rebuilt it 200ms after any key, arrows
// included, which put the highlight back on the first add-in. It now sits above
// the menu as a combobox: it passes the navigation keys to the menu, and its
// aria-activedescendant names the highlighted add-in.

import AxeBuilder from '@axe-core/playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import type { Locator, Page } from '@playwright/test';

import { ruleSetTags, violationSummary } from '@utils/a11y';
import { sleep } from '@utils/constants';

const ADDINS_BUTTON = '#rstudio_addins_toolbar_button';
const SEARCH_LABEL = 'Search for addins';

// Outlasts the search box's 200ms rebuild timer several times over.
const REBUILD_WINDOW_MS = 1000;

// devtools, in required-packages.txt, provides five.
const MIN_ADDINS = 3;

// Asserts continuously for `durationMs`, failing on the first violation;
// expect.poll would pass on its first sample, before any rebuild.
async function expectHoldsFor(durationMs: number, assertions: () => Promise<void>): Promise<void> {
  await assertions();
  const deadline = Date.now() + durationMs;
  while (Date.now() < deadline) {
    await sleep(50);
    await assertions();
  }
}

// The popup holding the search box and the menu. The search text outlives the
// popup, so clear what an earlier test typed. The add-in registry is filled in
// the background after startup, so a menu opened too early can show "No addins
// found"; reopen until it has items.
async function openAddinsPopup(page: Page): Promise<Locator> {
  const popup = page.locator('.popupContent:visible').filter({
    has: page.getByLabel(SEARCH_LABEL, { exact: true }),
  });
  await expect(async () => {
    if (await popup.count() > 0)
      await page.keyboard.press('Escape');
    await page.locator(ADDINS_BUTTON).click();
    const search = popup.getByLabel(SEARCH_LABEL, { exact: true });
    if (await search.inputValue() !== '')
      await search.clear();
    await expect(popup.getByRole('menuitem').nth(MIN_ADDINS - 1)).toBeVisible({ timeout: 2000 });
  }).toPass({ timeout: 60_000 });
  return popup;
}

function highlightedItem(popup: Locator): Locator {
  return popup.locator('[role=menuitem].gwt-MenuItem-selected');
}

// Answers execute_r_addin without running anything, recording the add-ins asked for.
async function captureAddinRuns(page: Page): Promise<string[]> {
  const runs: string[] = [];
  await page.route('**/rpc/execute_r_addin', async (route) => {
    const params = (route.request().postDataJSON() as { params: string[] }).params;
    runs.push(params[0]);
    await route.fulfill({ contentType: 'application/json', body: '{"result":null}' });
  });
  return runs;
}

test.describe('Addins toolbar menu (#19552)', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await page.unrouteAll({ behavior: 'ignoreErrors' });
    await page.keyboard.press('Escape');
  });

  test('the arrow-key highlight stays on the chosen add-in', async ({
    rstudioPage: page,
  }) => {
    const popup = await openAddinsPopup(page);
    const items = popup.getByRole('menuitem');
    const third = await items.nth(2).textContent();

    await page.keyboard.press('Home');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    await expect(highlightedItem(popup)).toHaveText(third!);

    await expectHoldsFor(REBUILD_WINDOW_MS, async () => {
      expect(await highlightedItem(popup).textContent(),
        'a navigation key must not rebuild the menu and reset the highlight').toBe(third);
    });

    await page.keyboard.press('End');
    const last = await items.last().textContent();
    await expectHoldsFor(REBUILD_WINDOW_MS, async () => {
      expect(await highlightedItem(popup).textContent()).toBe(last);
    });
  });

  test('Enter runs the highlighted add-in, and a typed space does not', async ({
    rstudioPage: page,
  }) => {
    const runs = await captureAddinRuns(page);
    const popup = await openAddinsPopup(page);
    const search = popup.getByRole('combobox', { name: SEARCH_LABEL });

    await page.keyboard.press('ArrowDown');
    await expect(highlightedItem(popup)).toHaveCount(1);
    await page.keyboard.type(' ');
    await expectHoldsFor(500, async () => {
      expect(runs, 'a space typed into the search box must not run an add-in').toEqual([]);
    });
    await expect(search).toHaveValue(' ');

    // A non-interactive add-in runs at once; an interactive one first checks for
    // Shiny. clipr comes with devtools, through usethis.
    await search.clear();
    await search.pressSequentially('Value to clipboard');
    await expect(popup.getByRole('menuitem')).toHaveText(['Value to clipboard']);
    await page.keyboard.press('ArrowDown');
    await expect(highlightedItem(popup)).toHaveText('Value to clipboard');
    await page.keyboard.press('Enter');
    await expect.poll(() => runs).toEqual(['clipr::clipr_result']);
  });

  test('Enter pressed right after typing runs the first match, not the old highlight', async ({
    rstudioPage: page,
  }) => {
    const runs = await captureAddinRuns(page);
    await openAddinsPopup(page);
    await page.keyboard.press('Home');

    // Typed and entered within the search box's 200ms rebuild delay. A slow run
    // that outlasts the delay passes too, without exercising the race.
    await page.keyboard.type('Output to clipboard');
    await page.keyboard.press('Enter');
    await expect.poll(() => runs).toEqual(['clipr::clipr_output']);
  });

  test('Shift+Home selects the search text instead of moving the highlight', async ({
    rstudioPage: page,
  }) => {
    const popup = await openAddinsPopup(page);
    const search = popup.getByRole('combobox', { name: SEARCH_LABEL });
    await search.pressSequentially('clip');
    await page.keyboard.press('Shift+Home');
    expect(await search.evaluate((el: HTMLInputElement) =>
      [el.selectionStart, el.selectionEnd])).toEqual([0, 4]);
  });

  // The menu focuses itself as it opens. In Safari, GWT's FocusImplSafari defers
  // that focus with setTimeout, so it could land after the search box took focus.
  // The fixtures run Chromium, which focuses at once, so replay Safari's deferral.
  test('opening the menu from the keyboard focuses the search box', async ({
    rstudioPage: page,
  }) => {
    await openAddinsPopup(page);
    await page.keyboard.press('Escape');
    const button = page.locator(ADDINS_BUTTON);
    await expect(button).toBeFocused();

    await page.evaluate(() => {
      const focus = HTMLElement.prototype.focus;
      (window as unknown as { __pwFocus: typeof focus }).__pwFocus = focus;
      HTMLElement.prototype.focus = function (this: HTMLElement, options?: FocusOptions) {
        if (this.getAttribute('role') === 'menu')
          setTimeout(() => focus.call(this, options), 0);
        else
          focus.call(this, options);
      };
    });
    try {
      await page.keyboard.press('Enter');
      const popup = page.locator('.popupContent:visible').filter({
        has: page.getByLabel(SEARCH_LABEL, { exact: true }),
      });
      const search = popup.getByRole('combobox', { name: SEARCH_LABEL });
      await expect(search).toBeFocused();
      await expectHoldsFor(REBUILD_WINDOW_MS, async () => {
        expect(await search.evaluate((el) => el === document.activeElement),
          'the menu\'s deferred focus must not take focus from the search box').toBe(true);
      });
      const firstId = await popup.getByRole('menuitem').first().getAttribute('id');
      await expect(search).toHaveAttribute('aria-activedescendant', firstId!);
    } finally {
      await page.evaluate(() => {
        HTMLElement.prototype.focus =
          (window as unknown as { __pwFocus: typeof HTMLElement.prototype.focus }).__pwFocus;
      });
    }
  });

  test('closing the menu returns focus to the Addins button', async ({
    rstudioPage: page,
  }) => {
    const button = page.locator(ADDINS_BUTTON);
    const focusedId = () => page.evaluate(() => document.activeElement?.id ?? '');
    await button.focus();
    await page.keyboard.press('Tab');
    const next = await focusedId();
    await button.focus();
    await page.keyboard.press('Shift+Tab');
    const previous = await focusedId();
    expect(next, 'the Addins button needs a focusable neighbor on each side').not.toBe('');
    expect(previous).not.toBe('');

    const popup = await openAddinsPopup(page);
    await page.keyboard.press('Tab');
    await expect(popup).toBeHidden();
    expect(await focusedId()).toBe(next);

    await openAddinsPopup(page);
    await page.keyboard.press('Shift+Tab');
    await expect(popup).toBeHidden();
    expect(await focusedId()).toBe(previous);

    await openAddinsPopup(page);
    await page.keyboard.press('Escape');
    await expect(popup).toBeHidden();
    await expect(button).toBeFocused();
  });

  test('the search box names the highlighted add-in to screen readers', async ({
    rstudioPage: page,
  }) => {
    const popup = await openAddinsPopup(page);
    const search = popup.getByRole('combobox', { name: SEARCH_LABEL });
    await expect(search).toBeFocused();
    await expect(search).toBeEnabled();
    await expect(search).toHaveAttribute('aria-expanded', 'true');
    const controls = await search.getAttribute('aria-controls');
    const menu = page.locator(`#${controls}`);
    await expect(menu).toHaveAttribute('role', 'menu');

    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    const highlighted = highlightedItem(popup);
    await expect(highlighted).toHaveCount(1);
    const id = await highlighted.getAttribute('id');
    await expect(search).toHaveAttribute('aria-activedescendant', id!);

    // A menu may contain only items, groups, and separators.
    const results = await new AxeBuilder({ page })
      .include(`#${controls}`)
      .withTags(ruleSetTags({ level: 'AA', version: '2.2' }))
      .analyze();
    expect(violationSummary(results)).toEqual([]);

    // With nothing left to highlight, the search box must not name a removed item.
    await search.pressSequentially('zzz-no-such-addin');
    await expect(popup.getByText('No addins found')).toBeVisible();
    await expect(search).not.toHaveAttribute('aria-activedescendant');
  });
});
