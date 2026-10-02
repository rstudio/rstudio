// The Tutorial pane's filter box narrows the home page's list of tutorials
// (#10173, #7566). Every term (separated by whitespace or ASCII punctuation)
// must match somewhere in a tutorial's title, package name, or tutorial name;
// the match ignores case.
//
// The expectations are written against the tutorials learnr itself ships
// (ex-data-basics, hello, ...); globalSetup preinstalls learnr from
// required-packages.txt. They are spelled out rather than derived from a copy
// of the matching rule so that a wrong rule can't be mirrored into the oracle.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { FrameLocator, Page } from '@playwright/test';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

const TUTORIAL_FRAME = '#rstudio_tutorial_frame';
const FILTER_INPUT = '#rstudio_sw_tutorial input';
const STOP_BUTTON = "[id^='rstudio_tb_tutorialstop']";
const ENTRY = '.rstudio-tutorials-entry';
const VISIBLE_ENTRY = `${ENTRY}:visible`;
const EMPTY_MESSAGE = '#rstudio_tutorials_filter_empty';

// Indexing the installed tutorials runs after the session starts, and the
// home page refreshes itself once that finishes.
const INDEX_TIMEOUT = 60_000;

// A tutorial renders in a background job; the first render on CI knits the
// R Markdown from scratch.
const TUTORIAL_START_TIMEOUT = 120_000;

type Entry = { pkg: string; name: string; title: string };

function tutorialFrame(page: Page): FrameLocator {
  return page.frameLocator(TUTORIAL_FRAME);
}

async function readEntries(frame: FrameLocator, selector = ENTRY): Promise<Entry[]> {
  return frame.locator(selector).evaluateAll((els) =>
    els.map((el) => ({
      pkg: el.getAttribute('data-tutorial-package') ?? '',
      name: el.getAttribute('data-tutorial-name') ?? '',
      title: el.getAttribute('data-tutorial-title') ?? '',
    })),
  );
}

function learnrTutorials(entries: Entry[], ...names: string[]): Entry[] {
  return entries.filter((e) => e.pkg === 'learnr' && names.includes(e.name));
}

// Type the text so the filter sees it the way a user's keystrokes arrive; the
// paste test covers edits that bypass the keyboard.
async function setFilter(page: Page, text: string): Promise<void> {
  const input = page.locator(FILTER_INPUT);
  await input.click();
  await input.press('ControlOrMeta+a');
  await input.press('Backspace');
  if (text.length > 0)
    await input.pressSequentially(text);
}

// A paste from the context menu (or a drag-and-drop) changes the value with
// an input event but no key, which is the one way to deliver text that
// setFilter() does not.
async function pasteFilter(page: Page, text: string): Promise<void> {
  await page.locator(FILTER_INPUT).evaluate((el: HTMLInputElement, value: string) => {
    el.value = value;
    el.dispatchEvent(new Event('input', { bubbles: true }));
  }, text);
}

test.describe('Tutorial pane filter', () => {
  let learnrAvailable = false;
  let tutorialDepsAvailable = false;
  let entries: Entry[] = [];

  test.beforeAll(async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    // The tutorials are indexed as the session starts, so learnr has to be in
    // place by then (required-packages.txt puts it there): a copy installed
    // now would not be listed. ensurePackage() still runs to fill in any
    // missing dependency, which the pane would otherwise prompt for.
    const preinstalled = (await consoleActions.evalRLogical('requireNamespace("learnr", quietly = TRUE)')) === true;
    learnrAvailable = preinstalled && (await consoleActions.ensurePackage('learnr', 180_000));
    // Starting a tutorial also needs rstudioapi; without it the pane prompts
    // to install it instead of launching.
    tutorialDepsAvailable = learnrAvailable && (await consoleActions.ensurePackage('rstudioapi', 120_000));
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

  test('matches part of a tutorial name, ignoring case', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'learnr must be preinstalled (required-packages.txt)');

    const frame = tutorialFrame(page);
    const expected = learnrTutorials(entries, 'ex-data-basics', 'ex-data-filter', 'ex-data-mutate', 'ex-data-summarise');
    expect(expected.length).toBeGreaterThan(0);
    expect(expected.length).toBeLessThan(entries.length);

    await setFilter(page, 'EX-DATA');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(expected.length);
    expect(await readEntries(frame, VISIBLE_ENTRY)).toEqual(expected);
    await expect(frame.locator(EMPTY_MESSAGE)).toBeHidden();
  });

  test('every term must match somewhere', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'learnr must be preinstalled (required-packages.txt)');

    const frame = tutorialFrame(page);
    const learnrCount = entries.filter((e) => e.pkg === 'learnr').length;
    const expected = learnrTutorials(entries, 'ex-data-basics');
    expect(expected).toHaveLength(1);

    await setFilter(page, 'learnr');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(learnrCount);

    // adding a word from the title narrows the list
    await setFilter(page, 'learnr basics');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(1);
    expect(await readEntries(frame, VISIBLE_ENTRY)).toEqual(expected);
  });

  test('accepts the "package: name" line as displayed', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'learnr must be preinstalled (required-packages.txt)');

    const frame = tutorialFrame(page);
    const expected = learnrTutorials(entries, 'hello');
    expect(expected).toHaveLength(1);

    await setFilter(page, 'learnr: hello');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(1);
    expect(await readEntries(frame, VISIBLE_ENTRY)).toEqual(expected);
  });

  test('filters on a paste, which arrives without a key', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'learnr must be preinstalled (required-packages.txt)');

    const frame = tutorialFrame(page);
    const expected = learnrTutorials(entries, 'hello');
    expect(expected).toHaveLength(1);

    await pasteFilter(page, 'learnr: hello');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(1);
    expect(await readEntries(frame, VISIBLE_ENTRY)).toEqual(expected);
  });

  test('keeps non-ASCII text together as one term', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'learnr must be preinstalled (required-packages.txt)');

    // No installed package ships a non-Latin tutorial, so add two entries to
    // the page by hand. The home page is regenerated on every load, so a
    // reload (the index finishing a late pass, say) drops them again; the
    // check below re-adds them and retries if that happens underneath it.
    const frame = tutorialFrame(page);
    const addEntries = () =>
      frame.locator('.rstudio-tutorials-container').evaluate((container) => {
        if (container.querySelector('[data-tutorial-package="exemples"]'))
          return;
        const add = (name: string, title: string) => {
          const el = document.createElement('div');
          el.className = 'rstudio-tutorials-section rstudio-tutorials-entry';
          el.setAttribute('data-tutorial-package', 'exemples');
          el.setAttribute('data-tutorial-name', name);
          el.setAttribute('data-tutorial-title', title);
          el.textContent = title;
          container.appendChild(el);
        };
        add('donnees', 'Les données');
        add('donner', 'Donner des exemples');
      });

    // "données" must not be split into "donn" and "es" (which would also
    // match "Donner des exemples")
    await expect(async () => {
      await addEntries();
      await setFilter(page, 'Données');
      expect(await readEntries(frame, VISIBLE_ENTRY)).toEqual([
        { pkg: 'exemples', name: 'donnees', title: 'Les données' },
      ]);
    }).toPass();

    // a non-Latin query is a term too, not an empty filter
    await setFilter(page, '数据');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(0);
    await expect(frame.locator(EMPTY_MESSAGE)).toBeVisible();

    // Refresh is only enabled while a tutorial is shown; Home reloads the page
    await executeCommand(page, 'tutorialHome');
    await expect(frame.locator(`${ENTRY}[data-tutorial-package="exemples"]`)).toHaveCount(0);
  });

  test('shows a message when nothing matches, and clears', async ({ rstudioPage: page }) => {
    test.skip(!learnrAvailable, 'learnr must be preinstalled (required-packages.txt)');

    const frame = tutorialFrame(page);
    await setFilter(page, 'no-such-tutorial-anywhere');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(0);
    await expect(frame.locator(EMPTY_MESSAGE)).toBeVisible();
    // announced to screen readers, since focus stays in the filter box
    await expect(frame.locator(EMPTY_MESSAGE)).toHaveAttribute('role', 'status');

    await setFilter(page, '');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(entries.length);
    await expect(frame.locator(EMPTY_MESSAGE)).toBeHidden();
  });

  test('hides the box while a tutorial runs and re-applies the filter after Home', async ({ rstudioPage: page }) => {
    test.skip(!tutorialDepsAvailable, 'learnr must be preinstalled (required-packages.txt) and rstudioapi available');

    const frame = tutorialFrame(page);
    const expected = learnrTutorials(entries, 'hello');
    expect(expected).toHaveLength(1);

    await setFilter(page, 'learnr: hello');
    await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(1);

    const consoleActions = new ConsolePaneActions(page);
    try {
      await frame.locator(`${ENTRY}[data-tutorial-name="hello"] button`).click();

      // the box goes once the loading page arrives, before the tutorial renders
      await expect(page.locator(FILTER_INPUT)).toBeHidden();
      await expect(page.locator(STOP_BUTTON)).toBeVisible({ timeout: TUTORIAL_START_TIMEOUT });
      await expect(page.locator(FILTER_INPUT)).toBeHidden();

      await executeCommand(page, 'tutorialHome');
      await expect(page.locator(FILTER_INPUT)).toBeVisible();
      await expect(page.locator(FILTER_INPUT)).toHaveValue('learnr: hello');
      await expect(frame.locator(VISIBLE_ENTRY)).toHaveCount(1);
      expect(await readEntries(frame, VISIBLE_ENTRY)).toEqual(expected);
    } finally {
      // Home leaves the tutorial's job running; stop it so it doesn't outlive
      // the test (Stop is only offered while the tutorial itself is shown).
      await consoleActions.executeInConsole(
        'invisible(tryCatch(.rs.tutorial.stopTutorial("hello", "learnr"), error = function(e) NULL))',
      );
    }
  });
});
