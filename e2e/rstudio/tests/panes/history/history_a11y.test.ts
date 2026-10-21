// The History pane's lists are named listboxes whose selection is exposed to
// assistive technology, and the views not on screen are inert (#19207).

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand, getPref, setPref } from '@utils/commands';
import type { Locator, Page } from 'playwright';

const HISTORY_PANEL = '#rstudio_workbench_panel_history';
const SEARCH_INPUT = '#rstudio_sw_history input';

// Unique per worker: console history persists across the sessions a run
// starts, so a fixed marker would also match an earlier worker's commands.
const MARKER = `hist_a11y_${Date.now().toString(36)}`;
const COMMANDS = [1, 2, 3].map(i => `${MARKER}_${i} <- ${i}`);

function panel(page: Page): Locator {
  return page.locator(HISTORY_PANEL);
}

function listbox(page: Page, name: string): Locator {
  return panel(page).getByRole('listbox', { name, exact: true });
}

// The option the listbox reports as active, resolved through its
// aria-activedescendant id.
async function activeOption(list: Locator): Promise<{ text: string; selected: string | null }> {
  return list.evaluate(el => {
    const id = el.getAttribute('aria-activedescendant');
    const option = id ? document.getElementById(id) : null;
    if (!option || !el.contains(option) || option.getAttribute('role') !== 'option')
      return { text: '', selected: null };
    return {
      text: (option.textContent ?? '').trim(),
      selected: option.getAttribute('aria-selected'),
    };
  });
}

async function selectedOptionTexts(list: Locator): Promise<string[]> {
  return list.getByRole('option', { selected: true }).allTextContents();
}

// Visible buttons in the History pane whose label is `name`. Buttons in the
// views that aren't shown must be inert, so the accessibility tree excludes them.
function exposedButtons(page: Page, name: string): Locator {
  return panel(page).getByRole('button', { name, exact: true });
}

// The search runs when the box's value changes (Enter would send the selected
// command to the console). Done leaves the query in place, so clear it first.
async function search(page: Page, term: string): Promise<void> {
  await page.locator(SEARCH_INPUT).fill('');
  await page.locator(SEARCH_INPUT).fill(term);
  await expect(listbox(page, 'Console History Search Results')
    .getByRole('option')).toHaveCount(COMMANDS.length);
}

async function showContext(page: Page): Promise<void> {
  await search(page, MARKER);
  const results = listbox(page, 'Console History Search Results');
  await results.getByRole('option').first().click();
  await executeCommand(page, 'historyShowContext');
  await expect(listbox(page, 'Console History Context')).toBeVisible();
  // The view slides in; the search results stay exposed until it ends.
  await expect(results).toHaveCount(0);
}

// Back slides from the context view to the search results; both stay exposed
// until the slide ends, so wait for it before looking for Done.
async function returnToRecent(page: Page): Promise<void> {
  if (await exposedButtons(page, 'Back').count() > 0) {
    await exposedButtons(page, 'Back').click();
    await expect(listbox(page, 'Console History Context')).toHaveCount(0);
  }
  if (await exposedButtons(page, 'Done').count() > 0)
    await exposedButtons(page, 'Done').click();
  await expect(listbox(page, 'Console History')).toBeVisible();
}

// Whether the focused element is somewhere a sighted keyboard user can see:
// not under an inert view and not parked off-screen.
async function focusIsOnScreen(page: Page): Promise<boolean> {
  return page.evaluate(() => {
    const el = document.activeElement;
    if (!el || el === document.body || el.closest('[inert]'))
      return false;
    const rect = el.getBoundingClientRect();
    return rect.right > 0 && rect.left < window.innerWidth;
  });
}

// Resolves on the first frame in which the context view is exposed, reporting
// whether the search results had already turned inert by then.
async function waitForContextShown(page: Page): Promise<{ searchResultsInert: boolean }> {
  const handle = await page.waitForFunction((panelSelector) => {
    const tables = document.querySelectorAll(`${panelSelector} table[role="listbox"]`);
    const byName = (name: string) =>
      [...tables].find(t => t.getAttribute('aria-label') === name);
    const context = byName('Console History Context');
    const results = byName('Console History Search Results');
    if (!context || !results || context.closest('[inert]'))
      return false;
    return { searchResultsInert: results.closest('[inert]') !== null };
  }, HISTORY_PANEL, { polling: 'raf' });
  return handle.jsonValue() as Promise<{ searchResultsInert: boolean }>;
}

test.describe('History pane accessibility', () => {
  // The fixture turns reduced motion on for every test; put back its value,
  // not the pref's default, so later tests in the worker keep it.
  let fixtureReducedMotion: boolean;

  test.beforeAll(async ({ rstudioPage: page }) => {
    // Unlike setPref, getPref doesn't wait out a session reload a previous
    // spec left in flight, during which the bridge's prefs are absent.
    await page.waitForFunction(() => window.rstudio?.prefs?.reducedMotion !== undefined);
    fixtureReducedMotion = await getPref(page, 'reduced_motion') as boolean;
    for (const command of COMMANDS)
      await executeInConsole(page, command);
  });

  // The search and context views slide in unless reduced motion is on.
  for (const reducedMotion of [false, true]) {
    test.describe(`reduced motion ${reducedMotion ? 'on' : 'off'}`, () => {
      test.beforeAll(async ({ rstudioPage: page }) => {
        await setPref(page, 'reduced_motion', reducedMotion);
      });

      test.afterAll(async ({ rstudioPage: page }) => {
        await setPref(page, 'reduced_motion', fixtureReducedMotion);
      });

      test.beforeEach(async ({ rstudioPage: page }) => {
        await executeCommand(page, 'activateHistory');
        await returnToRecent(page);
      });

      test('each history view is a named listbox', async ({ rstudioPage: page }) => {
        const recent = listbox(page, 'Console History');
        await expect(recent).toHaveAttribute('aria-multiselectable', 'true');
        await expect(recent.getByRole('option', { name: COMMANDS[2] })).toHaveCount(1);

        await search(page, MARKER);
        // Exact: the disclosure button's tooltip must stay out of the name.
        await expect(listbox(page, 'Console History Search Results')
          .getByRole('option', { name: COMMANDS[0], exact: true })).toHaveCount(1);

        await showContext(page);
        await expect(listbox(page, 'Console History Context')
          .getByRole('option', { name: COMMANDS[0] })).toHaveCount(1);
      });

      test('views that are not shown are inert', async ({ rstudioPage: page }) => {
        await expect(listbox(page, 'Console History Search Results')).toHaveCount(0);
        await expect(listbox(page, 'Console History Context')).toHaveCount(0);
        await expect(exposedButtons(page, 'Done')).toHaveCount(0);
        await expect(exposedButtons(page, 'Back')).toHaveCount(0);

        await search(page, MARKER);
        await expect(listbox(page, 'Console History')).toHaveCount(0);
        await expect(exposedButtons(page, 'Done')).toHaveCount(1);
        await expect(exposedButtons(page, 'Back')).toHaveCount(0);

        await showContext(page);
        await expect(listbox(page, 'Console History Search Results')).toHaveCount(0);
        await expect(exposedButtons(page, 'Done')).toHaveCount(1);
        await expect(exposedButtons(page, 'Back')).toHaveCount(1);
      });

      test('Tab from the list skips the hidden views', async ({ rstudioPage: page }) => {
        // Showing the context view once leaves its scroller with content, which
        // Chrome makes a Tab stop unless the view is inert.
        await showContext(page);
        await returnToRecent(page);

        await listbox(page, 'Console History').focus();
        await page.keyboard.press('Tab');
        expect(await focusIsOnScreen(page)).toBe(true);
      });

      test('Done and Back keep keyboard focus in the pane', async ({ rstudioPage: page }) => {
        await search(page, MARKER);
        await exposedButtons(page, 'Done').focus();
        await page.keyboard.press('Enter');
        await expect(listbox(page, 'Console History')).toBeFocused();

        await showContext(page);
        await exposedButtons(page, 'Back').focus();
        await page.keyboard.press('Enter');
        await expect(listbox(page, 'Console History Search Results')).toBeFocused();

        await showContext(page);
        await exposedButtons(page, 'Done').focus();
        await page.keyboard.press('Enter');
        await expect(listbox(page, 'Console History')).toBeFocused();
      });

      test('arrow keys move an announced, multi-select selection', async ({
        rstudioPage: page,
      }) => {
        const recent = listbox(page, 'Console History');
        await recent.getByRole('option', { name: COMMANDS[2] }).click();
        await expect.poll(() => activeOption(recent))
          .toEqual({ text: COMMANDS[2], selected: 'true' });

        await page.keyboard.press('Shift+ArrowUp');
        await expect.poll(() => activeOption(recent))
          .toEqual({ text: COMMANDS[1], selected: 'true' });
        expect(await selectedOptionTexts(recent)).toEqual([COMMANDS[1], COMMANDS[2]]);

        await page.keyboard.press('ArrowUp');
        await expect.poll(() => activeOption(recent))
          .toEqual({ text: COMMANDS[0], selected: 'true' });
        expect(await selectedOptionTexts(recent)).toEqual([COMMANDS[0]]);
        await expect(recent.getByRole('option', { name: COMMANDS[2] }))
          .toHaveAttribute('aria-selected', 'false');
      });

      test('context options are described by their timestamp', async ({ rstudioPage: page }) => {
        await showContext(page);
        const context = listbox(page, 'Console History Context');
        const option = context.getByRole('option', { name: COMMANDS[0] });

        // The timestamp heading the option's group: the nearest non-option row above it.
        const timestamp = await option.evaluate(el => {
          const rows = [...el.closest('table')!.rows];
          const header = rows.slice(0, rows.indexOf(el as HTMLTableRowElement)).reverse()
            .find(row => row.getAttribute('role') !== 'option');
          return (header?.textContent ?? '').trim();
        });
        expect(timestamp).not.toBe('');
        await expect(option).toHaveAccessibleDescription(timestamp);
      });
    });
  }

  test('reduced motion shows the context view without a slide', async ({ rstudioPage: page }) => {
    await setPref(page, 'reduced_motion', true);
    try {
      await executeCommand(page, 'activateHistory');
      await returnToRecent(page);
      await search(page, MARKER);
      await listbox(page, 'Console History Search Results').getByRole('option').first().click();
      const showing = waitForContextShown(page);
      await executeCommand(page, 'historyShowContext');
      // A slide leaves the search results exposed for its 300ms.
      expect(await showing).toEqual({ searchResultsInert: true });
    } finally {
      await setPref(page, 'reduced_motion', fixtureReducedMotion);
    }
  });
});
