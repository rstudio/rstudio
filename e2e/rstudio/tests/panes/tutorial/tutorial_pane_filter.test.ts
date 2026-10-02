// The Tutorial pane's filter box narrows the home page's list of tutorials
// (#10173, #7566). Every term (separated by whitespace or punctuation) must
// match somewhere in a tutorial's title, package name, or tutorial name; the
// match ignores case.
//
// The list comes from the installed packages' learnr tutorials, so these tests
// read the entries' data attributes rather than assuming a particular tutorial
// exists. learnr itself ships several, which is enough to exercise the filter.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { FrameLocator, Page } from '@playwright/test';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

const TUTORIAL_FRAME = '#rstudio_tutorial_frame';
const FILTER_INPUT = '#rstudio_sw_tutorial input';
const ENTRY = '.rstudio-tutorials-entry';
const EMPTY_MESSAGE = '#rstudio_tutorials_filter_empty';

// Indexing the installed tutorials runs after the session starts, and the
// home page refreshes itself once that finishes.
const INDEX_TIMEOUT = 60_000;

type Entry = { pkg: string; name: string; title: string };

function tutorialFrame(page: Page): FrameLocator {
  return page.frameLocator(TUTORIAL_FRAME);
}

async function readEntries(frame: FrameLocator): Promise<Entry[]> {
  return frame.locator(ENTRY).evaluateAll((els) =>
    els.map((el) => ({
      pkg: el.getAttribute('data-tutorial-package') ?? '',
      name: el.getAttribute('data-tutorial-name') ?? '',
      title: el.getAttribute('data-tutorial-title') ?? '',
    })),
  );
}

async function readVisibleEntries(frame: FrameLocator): Promise<Entry[]> {
  return frame.locator(`${ENTRY}:visible`).evaluateAll((els) =>
    els.map((el) => ({
      pkg: el.getAttribute('data-tutorial-package') ?? '',
      name: el.getAttribute('data-tutorial-name') ?? '',
      title: el.getAttribute('data-tutorial-title') ?? '',
    })),
  );
}

function matches(entry: Entry, filter: string): boolean {
  const haystack = `${entry.title} ${entry.pkg} ${entry.name}`.toLowerCase();
  return filter
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .every((term) => haystack.includes(term));
}

// The filter responds to keyup, so the text has to arrive as keystrokes.
async function setFilter(page: Page, text: string): Promise<void> {
  const input = page.locator(FILTER_INPUT);
  await input.click();
  await input.press('ControlOrMeta+a');
  await input.press('Backspace');
  if (text.length > 0)
    await input.pressSequentially(text);
}

test.describe('Tutorial pane filter', () => {
  let learnrAvailable = false;
  let entries: Entry[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    // 180s covers a cold install of learnr and its dependencies on CI.
    learnrAvailable = await consoleActions.ensurePackage('learnr', 180_000);
    await consoleActions.clearConsole();
  });

  // The per-test reset leaves another tab selected, so bring the pane back
  // (and its home page, with the list indexed) before every test.
  test.beforeEach(async ({ rstudioPage: page }) => {
    if (!learnrAvailable)
      return;

    await executeCommand(page, 'activateTutorial');
    await expect(page.locator(FILTER_INPUT)).toBeVisible();

    const frame = tutorialFrame(page);
    await expect(frame.locator(ENTRY).first()).toBeVisible({ timeout: INDEX_TIMEOUT });
    if (entries.length === 0)
      entries = await readEntries(frame);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    if (learnrAvailable && (await page.locator(FILTER_INPUT).isVisible()))
      await setFilter(page, '');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await executeCommand(page, 'activateEnvironment').catch(() => {});
  });

  test('filters by package name', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'required R package not available: learnr');

    const frame = tutorialFrame(page);
    const pkg = entries[0].pkg;
    const expected = entries.filter((e) => matches(e, pkg));

    await setFilter(page, pkg.toUpperCase());
    await expect(frame.locator(`${ENTRY}:visible`)).toHaveCount(expected.length);
    expect(await readVisibleEntries(frame)).toEqual(expected);
    await expect(frame.locator(EMPTY_MESSAGE)).toBeHidden();
  });

  test('every word must match somewhere', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'required R package not available: learnr');

    const frame = tutorialFrame(page);
    const target = entries[entries.length - 1];
    const word = (target.title || target.name).split(/\s+/)[0];
    const filter = `${target.pkg} ${word}`;
    const expected = entries.filter((e) => matches(e, filter));
    expect(expected).toContainEqual(target);

    await setFilter(page, filter);
    await expect(frame.locator(`${ENTRY}:visible`)).toHaveCount(expected.length);
    expect(await readVisibleEntries(frame)).toEqual(expected);
  });

  test('accepts the "package: name" line as displayed', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'required R package not available: learnr');

    const frame = tutorialFrame(page);
    const target = entries[0];
    const filter = `${target.pkg}: ${target.name}`;
    const expected = entries.filter((e) => matches(e, filter));
    expect(expected).toContainEqual(target);

    await setFilter(page, filter);
    await expect(frame.locator(`${ENTRY}:visible`)).toHaveCount(expected.length);
    expect(await readVisibleEntries(frame)).toEqual(expected);
  });

  test('shows a message when nothing matches, and clears', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'required R package not available: learnr');

    const frame = tutorialFrame(page);
    await setFilter(page, 'no-such-tutorial-anywhere');
    await expect(frame.locator(`${ENTRY}:visible`)).toHaveCount(0);
    await expect(frame.locator(EMPTY_MESSAGE)).toBeVisible();

    await setFilter(page, '');
    await expect(frame.locator(`${ENTRY}:visible`)).toHaveCount(entries.length);
    await expect(frame.locator(EMPTY_MESSAGE)).toBeHidden();
  });

  test('filter survives returning to the home page', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'required R package not available: learnr');

    const frame = tutorialFrame(page);
    const pkg = entries[0].pkg;
    const expected = entries.filter((e) => matches(e, pkg));

    await setFilter(page, pkg);
    await expect(frame.locator(`${ENTRY}:visible`)).toHaveCount(expected.length);

    // Home reloads the home page; the filter should be applied again. Mark the
    // current document so the assertion waits for the reloaded one.
    await frame.locator('body').evaluate((body) => body.setAttribute('data-pw-stale', '1'));
    await executeCommand(page, 'tutorialHome');
    await expect(frame.locator('body[data-pw-stale]')).toHaveCount(0);
    await expect(frame.locator(`${ENTRY}:visible`)).toHaveCount(expected.length);
    expect(await readVisibleEntries(frame)).toEqual(expected);
  });
});
