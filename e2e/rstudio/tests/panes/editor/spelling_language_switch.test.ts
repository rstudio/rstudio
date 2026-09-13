// Switching the spelling dictionary in one step, with immediate effect
// (#12223). Edit > Change Spelling Language... opens a small picker; applying
// it updates the spelling_dictionary_language pref and the realtime spell
// checker re-checks open documents, so "colour" stops being flagged as soon
// as the dictionary becomes British English -- no restart.

import type { Page, Route } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { TIMEOUTS } from '@utils/constants';
import {
  clearPref,
  dismissAllModals,
  executeCommand,
  getPref,
  resetSourcePaneState,
  saveDocument,
  setPref,
  waitForSourcePaneReset,
} from '@utils/commands';
import { closeAndDeleteSandboxFiles, openFile, writeAndOpenFile } from '@utils/files';
import { closeProjectIfOpen, createAndOpenProject } from '@utils/project';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

const LANGUAGE_SELECT = '#rstudio_change_spelling_language_select';
const DIALOG_OK = '#rstudio_dlg_ok';
const CHECK_SPELLING = /\/rpc\/check_spelling(?:\?|$)/;

// "colour" sits at columns 4-10 of the first line
const CONTENT = 'The colour of the sky.\n';
const WORD_ROW = 0;
const WORD_START = 4;
const WORD_END = 10;

// A realtime spelling marker is a front "text" marker covering exactly the
// word (lint markers are added in front; session.getMarkers() alone would only
// return the back markers, e.g. the selected-word highlight).
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

// Prime background lint once before changing the dictionary. Assertions after
// the change must not edit the document: doing so would hide a missing recheck.
async function primeSpelling(
  page: Page,
  sourceActions: SourcePaneActions,
  editor: AceEditor,
): Promise<void> {
  if (!(await editor.isFocused())) {
    await sourceActions.sourcePane.aceTextInput.click({ force: true });
    await expect.poll(() => editor.isFocused()).toBe(true);
  }
  await editor.gotoLine(2, 0);
  await page.keyboard.type(' ');
  await page.keyboard.press('Backspace');
  await saveDocument(page);
}

async function expectWordFlagged(editor: AceEditor, flagged: boolean): Promise<void> {
  await expect.poll(() => isWordFlagged(editor), { timeout: TIMEOUTS.fileOpen }).toBe(flagged);
}

async function changeLanguage(page: Page, langId: string): Promise<void> {
  await executeCommand(page, 'changeSpellingLanguage');
  const dialog = page.getByRole('dialog', { name: 'Change Spelling Language', exact: true });
  await expect(dialog).toBeVisible();
  await dialog.locator(LANGUAGE_SELECT).selectOption(langId);
  await dialog.locator(DIALOG_OK).click();
  await expect(dialog).toBeHidden();
  await expect.poll(() => getPref(page, 'spelling_dictionary_language')).toBe(langId);
}

async function expectSpellCheckComplete(page: Page): Promise<void> {
  await executeCommand(page, 'checkSpelling');
  const complete = page.getByRole('alertdialog', { name: 'Check Spelling', exact: true });
  await expect(complete).toContainText('Spell check is complete.');
  await complete.getByRole('button', { name: 'OK', exact: true }).click();
  await expect(complete).toBeHidden();
}

test.describe('Change Spelling Language', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  const fileName = 'spelling_language_switch.Rmd';

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await setPref(page, 'real_time_spellchecking', true);
  });

  // the per-test fixture resets the source pane, so the document is opened
  // per test rather than once for the suite
  test.beforeEach(async ({ rstudioPage: page }) => {
    await waitForSourcePaneReset(page);
    await setPref(page, 'spelling_dictionary_language', 'en_US');
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

  test('a British dictionary un-flags "colour" without a restart', async ({ rstudioPage: page }) => {
    const editor = new AceEditor(page, '');
    await primeSpelling(page, sourceActions, editor);
    await expectWordFlagged(editor, true);

    await changeLanguage(page, 'en_GB');
    await expectWordFlagged(editor, false);
    await expectSpellCheckComplete(page);
  });

  test('switching back to American English flags it again', async ({ rstudioPage: page }) => {
    const editor = new AceEditor(page, '');
    await primeSpelling(page, sourceActions, editor);
    await expectWordFlagged(editor, true);
    await changeLanguage(page, 'en_GB');
    await expectWordFlagged(editor, false);

    await changeLanguage(page, 'en_US');
    await expectWordFlagged(editor, true);
  });

  test('a project dictionary change reaches the server and persists in the project', async ({ rstudioPage: page }) => {
    // Choosing the existing global value still needs to update the effective
    // server dictionary when a project overrides it.
    await setPref(page, 'spelling_dictionary_language', 'en_GB');
    const projectDir = await createAndOpenProject(
      page,
      sandbox.dir,
      'SpellingLanguage',
      ['SpellingDictionary: en_US'],
    );
    try {
      consoleActions = new ConsolePaneActions(page);
      sourceActions = new SourcePaneActions(page, consoleActions);
      await expect.poll(() => getPref(page, 'spelling_dictionary_language')).toBe('en_US');
      expect(await consoleActions.evalRLogical('.Call("rs_checkSpelling", "colour")')).toBe(false);
      await openFile(page, `${sandbox.dir}/${fileName}`);
      const editor = new AceEditor(page, '');
      await primeSpelling(page, sourceActions, editor);
      await expectWordFlagged(editor, true);

      await changeLanguage(page, 'en_GB');
      await expectWordFlagged(editor, false);
      expect(await consoleActions.evalRLogical('.Call("rs_checkSpelling", "colour")')).toBe(true);
      const projectFile = rPathLiteral(`${projectDir}/SpellingLanguage.Rproj`);
      expect(await consoleActions.evalRLogical(
        `any(readLines(${projectFile}) == "SpellingDictionary: en_GB")`,
      )).toBe(true);
    } finally {
      await dismissAllModals(page);
      await resetSourcePaneState(page);
      await closeProjectIfOpen(page);
      consoleActions = new ConsolePaneActions(page);
      sourceActions = new SourcePaneActions(page, consoleActions);
    }
    await expect.poll(() => getPref(page, 'spelling_dictionary_language')).toBe('en_GB');
  });

  test('a delayed response from the old dictionary cannot restore stale spelling results', async ({ rstudioPage: page }) => {
    const editor = new AceEditor(page, '');
    await primeSpelling(page, sourceActions, editor);
    await expectWordFlagged(editor, true);

    let heldRoute: Route | undefined;
    let oldMisspelledIndex = -1;
    await page.route(CHECK_SPELLING, async (route) => {
      const words = route.request().postDataJSON().params[0] as string[];
      if (!heldRoute && words.includes('favour')) {
        // "colour" is already cached as incorrect. This request combines that
        // cached result with the not-yet-cached "favour", exposing stale data
        // in both the document cache and the shared spelling service cache.
        oldMisspelledIndex = words.indexOf('favour');
        heldRoute = route;
        return;
      }
      await route.continue();
    });

    try {
      await editor.setValue('The favour of the colour.\n');
      await primeSpelling(page, sourceActions, editor);
      await expect.poll(() => heldRoute !== undefined).toBe(true);

      // A real British response must arrive while the American response is
      // still held. This establishes the order without relying on sleeps.
      const britishResponse = page.waitForResponse((response) =>
        CHECK_SPELLING.test(response.url()) &&
        (response.request().postData() ?? '').includes('favour'),
      );
      await changeLanguage(page, 'en_GB');
      const britishResult = await (await britishResponse).json();
      expect(britishResult.error).toBeUndefined();
      expect(britishResult.result).toEqual([]);

      const oldResponse = page.waitForResponse((response) => response.request() === heldRoute!.request());
      await heldRoute!.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ result: [oldMisspelledIndex] }),
      });
      await (await oldResponse).finished();
      await expectSpellCheckComplete(page);

      // Reopen to discard the per-document cache and exercise the shared
      // service cache too. Neither word should regain its American verdict.
      await resetSourcePaneState(page);
      await openFile(page, `${sandbox.dir}/${fileName}`);
      await expectSpellCheckComplete(page);
    } finally {
      await page.unroute(CHECK_SPELLING);
      // Resolve a still-held request when an earlier assertion fails so the
      // test does not leave an outstanding spelling RPC during teardown.
      await heldRoute?.abort().catch(() => {});
    }
  });

  test('clearing a project dictionary rechecks an open detached editor', async ({ rstudioPage: page }) => {
    await setPref(page, 'spelling_dictionary_language', 'en_GB');
    await createAndOpenProject(page, sandbox.dir, 'DetachedSpelling', ['SpellingDictionary: en_US']);
    let satellite: Page | undefined;
    try {
      consoleActions = new ConsolePaneActions(page);
      sourceActions = new SourcePaneActions(page, consoleActions);
      await openFile(page, `${sandbox.dir}/${fileName}`);
      const detached = page.context().waitForEvent('page');
      await executeCommand(page, 'popoutDoc');
      satellite = await detached;
      await satellite.waitForURL(/view=source_window_/);

      // Source satellites have no automation bridge. Match the one document
      // by content using the Ace page object, and prime its own spelling cache.
      // The page event fires while the popup is still bootstrapping GWT, and
      // the marker lookup throws until an editor is mounted; expect.poll does
      // not retry a throwing callback, so retry the whole check instead.
      const editor = new AceEditor(satellite, 'The colour');
      await expect(async () => {
        expect(await editor.getValue()).toBe(CONTENT);
      }).toPass({ timeout: 30000 });
      await satellite.locator('.ace_text-input').first().click({ force: true });
      await editor.gotoLine(2, 0);
      await satellite.keyboard.type(' ');
      await satellite.keyboard.press('Backspace');
      await satellite.keyboard.press('ControlOrMeta+s');
      await expectWordFlagged(editor, true);

      await executeCommand(page, 'projectOptions');
      const options = page.getByRole('dialog', { name: 'Project Options', exact: true });
      await options.locator('#rstudio_label_spelling_options').click();
      const language = options.getByRole('combobox', { name: 'Main dictionary language:' });
      await expect(language).toHaveValue('en_US');
      await language.selectOption({ label: '(Default)' });
      await options.locator('#rstudio_preferences_confirm').click();
      await expect(options).toBeHidden();

      await expect.poll(() => getPref(page, 'spelling_dictionary_language')).toBe('en_GB');
      // No edits or spelling commands in the satellite after clearing the
      // override: its full project preference layer must remove the old key.
      await expectWordFlagged(editor, false);
      expect(await consoleActions.evalRLogical('.Call("rs_checkSpelling", "colour")')).toBe(true);
    } finally {
      await satellite?.close().catch(() => {});
      await dismissAllModals(page);
      await resetSourcePaneState(page);
      await closeProjectIfOpen(page);
      consoleActions = new ConsolePaneActions(page);
      sourceActions = new SourcePaneActions(page, consoleActions);
    }
  });

  test('newly installed dictionaries remain available when the picker reopens', async ({ rstudioPage: page }) => {
    const installDictionaries = /\/rpc\/install_all_dictionaries(?:\?|$)/;
    const savePreferences = /\/rpc\/set_user_prefs(?:\?|$)/;
    const installedId = 'x-pw-installed';
    const installedName = 'Installed test dictionary';
    let installCount = 0;
    let savedInstalledDictionary = false;

    await executeCommand(page, 'changeSpellingLanguage');
    const dialog = page.getByRole('dialog', { name: 'Change Spelling Language', exact: true });
    const language = dialog.locator(LANGUAGE_SELECT);
    const languages = await language.locator('option').evaluateAll((options) =>
      options
        .map((option) => ({ id: (option as HTMLOptionElement).value, name: option.textContent ?? '' }))
        .filter((option) => option.id !== ''),
    );
    expect(languages.some((entry) => entry.id === installedId)).toBe(false);

    // Installation and persistence are mocked at the RPC boundary: this test
    // exercises client context reuse without downloading files or asking the
    // backend spelling engine to open a synthetic dictionary.
    await page.route(installDictionaries, async (route) => {
      installCount++;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          result: {
            all_languages_installed: true,
            available_languages: [...languages, { id: installedId, name: installedName }],
            custom_dictionaries: [],
          },
        }),
      });
    });
    await page.route(savePreferences, async (route) => {
      if (route.request().postDataJSON().params[0].spelling_dictionary_language === installedId) {
        savedInstalledDictionary = true;
        await route.fulfill({ status: 200, contentType: 'application/json', body: '{"result":null}' });
      } else {
        await route.continue();
      }
    });

    try {
      // The last entry invokes install/update on machines with either set of
      // dictionaries. Its successful response introduces the synthetic one.
      await language.selectOption({ index: languages.length });
      await expect(language.locator(`option[value="${installedId}"]`)).toHaveText(installedName);
      await language.selectOption(installedId);
      await dialog.locator(DIALOG_OK).click();
      await expect(dialog).toBeHidden();
      expect(savedInstalledDictionary).toBe(true);
      await expect.poll(() => getPref(page, 'spelling_dictionary_language')).toBe(installedId);

      await executeCommand(page, 'changeSpellingLanguage');
      await expect(dialog).toBeVisible();
      await expect(language.locator(`option[value="${installedId}"]`)).toHaveText(installedName);
      await expect(language).toHaveValue(installedId);
      await expect(language.locator('option').last()).toHaveText('Update Dictionaries...');
      expect(installCount).toBe(1);
    } finally {
      await dismissAllModals(page);
      await page.unroute(installDictionaries);
      await page.unroute(savePreferences);
      // Reload the real server context so the simulated installed list cannot
      // affect subsequent tests in this worker. The mock never changed disk.
      await page.reload();
      await page.waitForFunction(() => window.rstudio?.ready === true);
    }
  });
});
