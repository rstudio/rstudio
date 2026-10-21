// The History pane's lists are named listboxes whose selection is exposed to
// assistive technology, and the views not on screen are inert (#19207).

import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand } from '@utils/commands';
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

test.describe('History pane accessibility', () => {
  test.beforeAll(async ({ rstudioPage: page }) => {
    for (const command of COMMANDS)
      await executeInConsole(page, command);
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
    await expect(listbox(page, 'Console History Search Results')).toBeVisible();

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

  test('arrow keys move an announced, multi-select selection', async ({ rstudioPage: page }) => {
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
