// Every save makes the session re-detect the document's extended type and
// fire source_extended_type_detected. Handling that event must not reset the
// editor when the type is unchanged (#19054: it used to clear the spelling and
// diagnostic markers the lint-on-save pass had just rendered), while the
// R Markdown toolbar must still pick up YAML changes, including under autosave
// (#7833).

import type { Page, Route } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { TIMEOUTS } from '@utils/constants';
import {
  clearPref,
  dismissAllModals,
  saveDocument,
  setPref,
  waitForSourcePaneReset,
} from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

const GET_EVENTS = /\/events\/get_events(?:\?|$)/;

// "colour" sits at columns 4-10 of the body line (row 4)
const CONTENT = heredoc`
  ---
  title: "Markers"
  output: html_document
  ---
  The colour of the sky.
`;
const WORD_ROW = 4;
const WORD_START = 4;
const WORD_END = 10;

// A realtime spelling marker is a front "text" marker covering exactly the word.
async function isWordFlagged(editor: AceEditor): Promise<boolean> {
  return (await editor.getMarkers(true)).some(
    (marker) =>
      marker.type === 'text' &&
      marker.range?.start.row === WORD_ROW &&
      marker.range.start.column === WORD_START &&
      marker.range.end.row === WORD_ROW &&
      marker.range.end.column === WORD_END,
  );
}

async function focusEditor(page: Page, sourceActions: SourcePaneActions, editor: AceEditor): Promise<void> {
  if (!(await editor.isFocused())) {
    await sourceActions.sourcePane.aceTextInput.click({ force: true });
    await expect.poll(() => editor.isFocused()).toBe(true);
  }
}

// A no-op edit on the trailing empty line marks the document dirty without
// touching the flagged word; the save then runs the lint-on-save pass.
async function editAndSave(page: Page, sourceActions: SourcePaneActions, editor: AceEditor): Promise<void> {
  await focusEditor(page, sourceActions, editor);
  await editor.gotoLine(CONTENT.split('\n').length + 1, 0);
  await page.keyboard.type(' ');
  await page.keyboard.press('Backspace');
  await saveDocument(page);
}

// Client events are dispatched incrementally and in order, so once console
// output printed by a command is visible, every event the session queued
// before that command ran has been handled too. The marker is split across
// cat() arguments so the command's own echo cannot match it.
let markerCount = 0;
async function waitForPendingClientEvents(consoleActions: ConsolePaneActions): Promise<void> {
  const marker = `[pw:events-${++markerCount}]`;
  const [head, tail] = marker.split(':');
  await consoleActions.executeInConsole(`cat("${head}:", "${tail}", sep = "")`, { wait: false });
  await expect(consoleActions.consolePane.consoleOutput).toContainText(marker, { timeout: TIMEOUTS.fileOpen });
}

test.describe('Extended type re-detection on save', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  const fileName = 'save_extended_type_redetect.Rmd';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await setPref(page, 'real_time_spellchecking', true);
    await setPref(page, 'spelling_dictionary_language', 'en_US');
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
    await writeAndOpenFile(page, sandbox.dir, fileName, CONTENT);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await dismissAllModals(page);
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [fileName]);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'spelling_dictionary_language');
    await clearPref(page, 'real_time_spellchecking');
  });

  test('markers rendered by a save survive the extended type event', async ({ rstudioPage: page }) => {
    const editor = new AceEditor(page, '');

    // Park the event long-poll unsent while the document is saved, so the
    // extended type event queues up server-side and is only delivered after
    // the lint-on-save pass has rendered the marker. The lint result arrives
    // on its own RPC and is not blocked. More than one poll can be parked:
    // the save RPC arms the event listener's 2 s watchdog, which abandons a
    // poll that has not returned by then and issues another.
    let parking = true;
    const parked: Route[] = [];
    await page.route(GET_EVENTS, async (route) => {
      if (parking) {
        parked.push(route);
        return;
      }
      await route.continue();
    });
    const release = async (): Promise<void> => {
      parking = false;
      for (const route of parked.splice(0)) {
        // a poll the watchdog already abandoned cannot be continued any more
        await route.continue().catch(() => {});
      }
    };

    try {
      // The poll in flight when the route was installed is not intercepted.
      // A console command makes it return (its events ride back on it) and
      // the client's next poll is parked. If the current poll was already
      // parked, the command's events simply wait at the server instead; in
      // both cases the client's only pending poll is a parked one afterwards.
      await consoleActions.executeInConsole('invisible(NULL)', { wait: false });
      await expect.poll(() => parked.length, { timeout: TIMEOUTS.fileOpen }).toBeGreaterThan(0);

      await editAndSave(page, sourceActions, editor);
      await expect.poll(() => isWordFlagged(editor), { timeout: TIMEOUTS.fileOpen }).toBe(true);

      await release();
      await waitForPendingClientEvents(consoleActions);

      expect(await isWordFlagged(editor)).toBe(true);
    } finally {
      // Never leave the event long-poll stranded when an assertion fails.
      await release();
      await page.unroute(GET_EVENTS);
    }
  });

  test('an idle autosave still refreshes the Knit menu after a YAML change', async ({ rstudioPage: page }) => {
    await setPref(page, 'auto_save_on_idle', 'commit');
    const editor = new AceEditor(page, '');
    const knitOptions = sourceActions.sourcePane.knitOptions;
    const githubItem = page.getByRole('menuitem', { name: 'Knit to github_document', exact: true });

    try {
      await focusEditor(page, sourceActions, editor);
      await knitOptions.click();
      await expect(page.getByRole('menuitem', { name: 'Knit to HTML', exact: true })).toBeVisible();
      await expect(githubItem).toBeHidden();
      await page.keyboard.press('Escape');
      await expect(githubItem).toBeHidden();

      // Replace the output format by typing, so the edit nudges the autosave
      // timer the way real typing does; then wait for the autosave to land.
      await focusEditor(page, sourceActions, editor);
      await editor.gotoLine(3, 0);
      await page.keyboard.press('Shift+End');
      await page.keyboard.type('output: github_document');
      await expect.poll(
        () => page.evaluate(() => window.rstudio?.documents.active()?.dirty ?? null),
        { timeout: TIMEOUTS.fileOpen },
      ).toBe(false);
      await waitForPendingClientEvents(consoleActions);

      await knitOptions.click();
      await expect(githubItem).toBeVisible();
      await page.keyboard.press('Escape');
    } finally {
      await clearPref(page, 'auto_save_on_idle');
    }
  });
});
