// Main toolbar > Addins: keyboard navigation and screen reader semantics (#19552).
//
// Focus stays in the menu's search box while the arrow keys move the highlight.
// The search box rebuilt the menu 200ms after any key, arrows included, which
// put the highlight back on the first add-in. It now also acts as a combobox
// whose aria-activedescendant names the highlighted add-in.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Locator, Page } from '@playwright/test';

import { sleep } from '@utils/constants';

const ADDINS_BUTTON = '#rstudio_addins_toolbar_button';

// Outlasts the search box's 200ms rebuild timer several times over.
const REBUILD_WINDOW_MS = 1000;

// devtools and styler, both in required-packages.txt, provide at least this many.
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

// The add-in registry is filled in the background after startup, so a menu
// opened too early can show "No addins found"; reopen until it has items.
async function openAddinsMenu(page: Page): Promise<Locator> {
  const menu = page.locator('[role=menu]:visible').filter({
    has: page.getByLabel('Search for addins', { exact: true }),
  });
  await expect(async () => {
    if (await menu.count() > 0)
      await page.keyboard.press('Escape');
    await page.locator(ADDINS_BUTTON).click();
    expect(await menu.getByRole('menuitem').count()).toBeGreaterThanOrEqual(MIN_ADDINS);
  }).toPass({ timeout: 60_000 });
  return menu;
}

function highlightedItem(menu: Locator): Locator {
  return menu.locator('[role=menuitem].gwt-MenuItem-selected');
}

test.describe('Addins toolbar menu (#19552)', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await page.keyboard.press('Escape');
  });

  test('the arrow-key highlight stays on the chosen add-in', async ({ rstudioPage: page }) => {
    const menu = await openAddinsMenu(page);
    const items = menu.getByRole('menuitem');
    const third = await items.nth(2).textContent();

    for (let i = 0; i < 3; i++)
      await page.keyboard.press('ArrowDown');
    await expect(highlightedItem(menu)).toHaveText(third!);

    await expectHoldsFor(REBUILD_WINDOW_MS, async () => {
      expect(await highlightedItem(menu).textContent(),
        'a navigation key must not rebuild the menu and reset the highlight').toBe(third);
    });

    await page.keyboard.press('End');
    const last = await items.last().textContent();
    await expectHoldsFor(REBUILD_WINDOW_MS, async () => {
      expect(await highlightedItem(menu).textContent()).toBe(last);
    });
  });

  test('the search box names the highlighted add-in to screen readers', async ({ rstudioPage: page }) => {
    const menu = await openAddinsMenu(page);
    const search = menu.getByRole('combobox', { name: 'Search for addins' });
    await expect(search).toBeFocused();
    await expect(search).toBeEnabled();
    await expect(search).toHaveAttribute('aria-expanded', 'true');
    const controls = await search.getAttribute('aria-controls');
    await expect(page.locator(`#${controls}`)).toHaveAttribute('role', 'menu');

    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    const highlighted = highlightedItem(menu);
    await expect(highlighted).toHaveCount(1);
    const id = await highlighted.getAttribute('id');
    await expect(search).toHaveAttribute('aria-activedescendant', id!);

    // With nothing left to highlight, the search box must not name a removed item.
    await search.pressSequentially('zzz-no-such-addin');
    await expect(menu.getByText('No addins found')).toBeVisible();
    await expect(search).not.toHaveAttribute('aria-activedescendant');
  });
});
