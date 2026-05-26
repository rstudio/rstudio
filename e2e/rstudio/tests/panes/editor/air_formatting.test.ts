/**
 * Air Formatting (#16721)
 *
 * Verifies that the Air formatter respects the "Use Air" checkbox.
 * Bug: Air formatting was always used if air.toml was found, even if
 * the global option was turned off.
 *
 * Fix: PR #17202 — requires all three conditions for Air:
 *   1. Formatter set to "none" (default)
 *   2. Air checkbox enabled
 *   3. air.toml present
 *
 * RStudio checks for both air.toml and .air.toml (hidden variant).
 *
 * Test matrix (10 cases):
 *   1.  unchecked + no config    + manual reformat  → built-in formatter
 *   2.  unchecked + air.toml     + manual reformat  → built-in formatter
 *   3.  unchecked + air.toml     + save              → code unchanged
 *   4.  checked   + air.toml     + manual reformat  → Air formatting
 *   5.  checked   + air.toml     + save              → Air formatting
 *   6.  checked   + no config    + manual reformat  → built-in formatter
 *   7.  unchecked + .air.toml    + manual reformat  → built-in formatter
 *   8.  unchecked + .air.toml    + save              → code unchanged
 *   9.  checked   + .air.toml    + manual reformat  → Air formatting
 *   10. checked   + .air.toml    + save              → Air formatting
 *
 * Detection: air.toml uses 12-space indent — unmistakable signal.
 */

import { expect } from '@playwright/test';
import type { Page } from 'playwright';
import { test } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, saveDocument, setPref } from '@utils/commands';
import { closeProjectIfOpen, createAndOpenProject } from '@utils/project';

// --- Test data (from issue #16721) ---

const TEST_FILE = `air_format_test_${Date.now()}.R`;
const AIR_TOML_FILE = 'air.toml'; // RStudio checks for both air.toml and .air.toml
const DOT_AIR_TOML_FILE = '.air.toml';

// Checkbox labels as they appear in Global Options > Code
// Source: UserPrefsAccessorConstants_en.properties
const AIR_CHECKBOX_LABEL = 'Use Air for code formatting';
const REFORMAT_ON_SAVE_LABEL = 'Reformat documents on save';

// Selectors derived from RStudio source (ElementIds.idFromLabel + SectionChooser)
// Name "Code" → idSafeString → "code" → "rstudio_label_code" + "_options"
const CODE_TAB = '#rstudio_label_code_options';
// Menu bar: AppMenuBar.addItem → idFromLabel("Code") + "_menu"
const CODE_MENU = '#rstudio_label_code_menu';
// Command: AppCommand.formatMenuLabel → idFromLabel("Reformat Selection") + "_command"
const REFORMAT_COMMAND = '#rstudio_label_reformat_selection_command';
const OPTIONS_OK = '#rstudio_preferences_confirm';

// --- Helpers ---

/** Open Global Options and navigate to Code > Formatting. */
async function openCodeOptions(page: Page): Promise<void> {
  await executeCommand(page, 'showOptions');
  await page.waitForSelector(OPTIONS_OK, { timeout: 15000 });
  await page.locator(CODE_TAB).click();
  // Click the "Formatting" sub-tab within Code options. setCheckbox waits for
  // its own target label to be visible, so no separate readiness gate is
  // needed here.
  await page.getByText('Formatting', { exact: true }).click();
}

/** Set a checkbox to a desired state (checked or unchecked), scoped to the Formatting tab. */
async function setCheckbox(page: Page, labelText: string, checked: boolean): Promise<void> {
  // Scope to the visible Formatting tab panel to avoid strict mode violations
  // ("Reformat documents on save" appears on both Formatting and Saving tabs)
  const formattingPanel = page.getByLabel('Formatting', { exact: true });
  const label = formattingPanel.locator(`xpath=.//label[contains(text(),"${labelText}")]`);
  // Wait for the label to be visible before reading or clicking. This covers
  // two cases: (1) the Formatting sub-tab content has finished laying out
  // after the tab switch, and (2) "Reformat documents on save" only appears
  // as a side effect of toggling "Use Air" on, so a caller switching it
  // immediately after needs to wait for it to surface.
  await label.waitFor({ state: 'visible', timeout: 10000 });
  const checkbox = formattingPanel.locator(`xpath=.//label[contains(text(),"${labelText}")]/../input`);
  const isChecked = await checkbox.isChecked().catch(() => false);
  if (isChecked !== checked) {
    await label.click();
    await expect.poll(() => checkbox.isChecked().catch(() => false), { timeout: 2000 }).toBe(checked);
  }
}

/** Close Global Options by clicking OK. */
async function closeOptions(page: Page): Promise<void> {
  const ok = page.locator(OPTIONS_OK);
  // OK -> PreferencesDialogBase.attemptSaveChanges -> setUserPrefs RPC ->
  // onResponseReceived -> closeDialog. The dialog only detaches on the RPC
  // callback, so toBeHidden has to outlast a server round-trip. Earlier
  // versions of this helper retried the click on a short toBeHidden window,
  // but the retry actually broke things: the first click DID work, the
  // hidden check just didn't wait long enough, and subsequent clicks hung
  // on an OK button that was already gone from the DOM. 15s covers the
  // RPC comfortably on a busy CI host.
  await ok.click();
  try {
    await expect(ok).toBeHidden({ timeout: 15000 });
  } catch (err) {
    // The wait timed out -- the prefs dialog never closed. Distinguish the
    // two known causes before re-raising so a recurring miss surfaces a
    // precise reproducer rather than a generic toBeHidden error.
    //
    //   1. A pane's validate() returned false. attemptSaveChanges silently
    //      no-ops in that case (the if-block in PreferencesDialogBase wraps
    //      both the RPC AND the closeDialog onCompleted), AND the failing
    //      pane pops up a #rstudio_dlg_ok error modal via
    //      GlobalDisplay.showErrorMessage. The prefs dialog stays open
    //      behind it forever.
    //
    //   2. Something else (slow RPC, modal stack from a prior failed test).
    const blockingModal = page.locator('#rstudio_dlg_ok');
    if (await blockingModal.isVisible().catch(() => false)) {
      const modalText = await page
        .locator('.gwt-DialogBox')
        .last()
        .innerText()
        .catch(() => '(unreadable)');
      throw new Error(
        'closeOptions: prefs dialog stayed open behind an error modal. ' +
        'Likely cause: a pane\'s validate() rejected a value (often a ' +
        'NumericTextBox that initialized to a blank/invalid value).\n' +
        '--- modal text ---\n' +
        modalText.slice(0, 500),
      );
    }
    throw err;
  }
}

/**
 * Set Air-related preferences via the Global Options dialog.
 * Opens Options > Code, sets both checkboxes, clicks OK.
 */
async function setAirPrefs(
  page: Page,
  useAir: boolean,
  reformatOnSave: boolean
): Promise<void> {
  await openCodeOptions(page);

  // "Use Air" must be set first -- "Reformat on save" is only visible when Air is checked.
  // setCheckbox already polls for the click to land, so no extra wait needed.
  await setCheckbox(page, AIR_CHECKBOX_LABEL, useAir);

  if (useAir) {
    // Reformat on save checkbox is now visible
    await setCheckbox(page, REFORMAT_ON_SAVE_LABEL, reformatOnSave);
  }

  await closeOptions(page);
}

/** Reset Air-related preferences programmatically (for setup/cleanup, not the test itself). */
async function resetAirPrefs(consoleActions: ConsolePaneActions): Promise<void> {
  await setPref(consoleActions.page, 'code_formatter', 'none');
  await setPref(consoleActions.page, 'use_air_formatter', false);
  await setPref(consoleActions.page, 'reformat_on_save', false);
}

/** Create an Air config file in the working directory. */
async function createAirConfig(consoleActions: ConsolePaneActions, fileName: string = AIR_TOML_FILE): Promise<void> {
  await consoleActions.executeInConsole(
    `writeLines(c("[format]", "line-width = 20", "indent-width = 12", 'indent-style = "space"', "persistent-line-breaks = true", 'exclude = ["tmp/", ".git/", "renv/"]', "default-exclude = true"), "${fileName}")`,
    { wait: true },
  );
}

/** Remove both air.toml and .air.toml if they exist. */
async function removeAirConfig(consoleActions: ConsolePaneActions): Promise<void> {
  await consoleActions.executeInConsole(
    `{ unlink("${AIR_TOML_FILE}"); unlink("${DOT_AIR_TOML_FILE}") }`,
    { wait: true },
  );
}

/** Create the test R file with messy code and open it in the editor. */
async function openTestFile(
  consoleActions: ConsolePaneActions,
  sourceActions: SourcePaneActions
): Promise<void> {
  await consoleActions.executeInConsole(
    `writeLines(c("x<-1+2+3", "y<-list(a=1,b=2,c=3)"), "${TEST_FILE}")`,
    { wait: true },
  );
  await consoleActions.executeInConsole(`file.edit("${TEST_FILE}")`);
  await expect(sourceActions.sourcePane.selectedTab).toContainText(TEST_FILE, { timeout: 10000 });
}

/** Click the editor content area to make sure it has focus. */
async function focusEditor(sourceActions: SourcePaneActions): Promise<void> {
  await sourceActions.sourcePane.contentPane.click();
  // Wait until focus has actually landed on a source-pane Ace textarea --
  // click() resolves before the focus event has propagated.
  await sourceActions.page.waitForFunction(
    () => {
      const el = document.activeElement;
      if (!el) return false;
      if (el.closest('#rstudio_console_input')) return false;
      return el.classList?.contains('ace_text-input') ?? false;
    },
    null,
    { timeout: 2000, polling: 50 },
  );
}

/** Select all code in the editor, then reformat via Cmd+Shift+A / Ctrl+Shift+A. */
async function reformatCode(page: Page, sourceActions: SourcePaneActions): Promise<void> {
  // Snapshot the content before the keystroke so we can poll for the diff
  // to land. The reformat command (whether server-side Air or client-side
  // built-in) applies its diff asynchronously and exposes no per-command
  // signal; every caller of this helper expects the content to *change*,
  // so a content-change poll is a precise readiness signal. Callers that
  // need to assert "no change" should use saveFile (which polls dirty=false
  // via saveDocument) instead.
  const before = await sourceActions.getEditorContent();
  await focusEditor(sourceActions);
  await page.keyboard.press('ControlOrMeta+a'); // select all
  await page.keyboard.press('ControlOrMeta+Shift+a'); // reformat selection
  await expect.poll(
    () => sourceActions.getEditorContent(),
    { timeout: 10000 },
  ).not.toBe(before);
}

/** Save the current file via Cmd+S / Ctrl+S (triggers reformat-on-save if enabled). */
async function saveFile(page: Page, sourceActions: SourcePaneActions): Promise<void> {
  await focusEditor(sourceActions);
  await sourceActions.goToEnd();
  await page.keyboard.press('Enter');
  await page.keyboard.type('z<-4');
  // saveDocument polls documents.active().dirty until the save (and any
  // pre-save formatter pass) has fully applied, so callers can read the
  // editor value next without further waits.
  await saveDocument(page);
}

/** Close the test file and clean up. */
async function cleanup(
  consoleActions: ConsolePaneActions,
  sourceActions: SourcePaneActions
): Promise<void> {
  await sourceActions.closeSourceAndDeleteFile(TEST_FILE);
  await removeAirConfig(consoleActions);
  // Reset prefs programmatically (not the thing being tested)
  await resetAirPrefs(consoleActions);
}

// --- Assertions ---

/** Air formatted: 12-space indentation is the unmistakable signal. */
function expectAirFormatted(content: string): void {
  expect(content).toContain('            '); // 12 spaces
  expect(content).not.toContain('x<-1+2+3');
}

/** Built-in formatted: spaces added around operators, no 12-space indent. */
function expectBuiltinFormatted(content: string): void {
  expect(content).not.toContain('            '); // no 12 spaces
  expect(content).toContain('x <- 1 + 2 + 3');
}

/** Code is unchanged — neither formatter ran. */
function expectUnchanged(content: string): void {
  expect(content).toContain('x<-1+2+3');
}

// --- Tests ---

test.describe('Air Formatting (#16721)', { tag: ['@parallel_safe'] }, () => {
  // Sets cwd to a per-spec sandbox; the relative paths used throughout this
  // spec (TEST_FILE, air.toml, .air.toml) all resolve into that sandbox.
  const sandbox = useSuiteSandbox();
  let page: Page;
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage }) => {
    page = rstudioPage;
    // The save-triggered reformat path in TextEditingTarget.maybeFormatOnUserInitiatedSave
    // is gated on "file is inside the active project" -- without a project,
    // reformat-on-save is skipped entirely, so cases 5/10 can't pass. Open a
    // project inside the suite sandbox; the project open also re-`setwd`s into
    // the project dir, so relative paths still resolve consistently and air.toml
    // ends up alongside the file being formatted (which is what Air's ancestor
    // walk needs to find it).
    await createAndOpenProject(page, sandbox.dir, 'air_formatting');
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.closeAllBuffersWithoutSaving();
    await consoleActions.clearConsole();
    // Start clean — programmatic reset (not the thing being tested)
    await removeAirConfig(consoleActions);
    await resetAirPrefs(consoleActions);
  });

  test.afterEach(async () => {
    await cleanup(consoleActions, sourceActions);
  });

  // Close the project in afterAll so downstream test files don't inherit a
  // project context. TextEditingTarget.maybeFormatOnUserInitiatedSave (and
  // similar gates) consult projConfig_.stripTrailingWhitespace() when a
  // project is active, which silently overrides the global user pref --
  // e.g. editor.test.ts's strip-trailing-whitespace test sets the global
  // pref and expects it to take effect, which it can't if our project is
  // still open with the project-level value defaulting to FALSE.
  test.afterAll(async () => {
    await closeProjectIfOpen(page);
  });

  test('1: baseline — unchecked, no air.toml, manual reformat uses built-in formatter', async () => {
    // Prefs: useAir=false, reformatOnSave=false (already default from beforeAll)
    // No air.toml (already absent)
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  test('2: unchecked, air.toml present, manual reformat uses built-in formatter', async () => {
    await setAirPrefs(page, false, false);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  test('3: unchecked, air.toml present, reformat on save leaves code unchanged', async () => {
    await setAirPrefs(page, false, false);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectUnchanged(content);
  });

  test('4: checked, air.toml present, manual reformat uses Air', async () => {
    await setAirPrefs(page, true, false);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

  test('5: checked, air.toml present, save uses Air', async () => {
    await setAirPrefs(page, true, true);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

  test('6: checked, no config, manual reformat uses built-in formatter', async () => {
    await setAirPrefs(page, true, false);
    await removeAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  // --- .air.toml (hidden variant) tests ---

  test('7: unchecked, .air.toml present, manual reformat uses built-in formatter', async () => {
    await setAirPrefs(page, false, false);
    await createAirConfig(consoleActions, DOT_AIR_TOML_FILE);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  test('8: unchecked, .air.toml present, reformat on save leaves code unchanged', async () => {
    await setAirPrefs(page, false, false);
    await createAirConfig(consoleActions, DOT_AIR_TOML_FILE);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectUnchanged(content);
  });

});
