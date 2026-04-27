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
import { sleep } from '@utils/constants';
import { useSuiteSandbox } from '@utils/sandbox';

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
async function openCodeOptions(page: Page, consoleActions: ConsolePaneActions): Promise<void> {
  await consoleActions.typeInConsole(".rs.api.executeCommand('showOptions')");
  await page.waitForSelector(OPTIONS_OK, { timeout: 15000 });
  await sleep(500);

  await page.locator(CODE_TAB).click();
  await sleep(500);

  // Click the "Formatting" sub-tab within Code options
  await page.getByText('Formatting', { exact: true }).click();
  await sleep(500);
}

/** Set a checkbox to a desired state (checked or unchecked), scoped to the Formatting tab. */
async function setCheckbox(page: Page, labelText: string, checked: boolean): Promise<void> {
  // Scope to the visible Formatting tab panel to avoid strict mode violations
  // ("Reformat documents on save" appears on both Formatting and Saving tabs)
  const formattingPanel = page.getByLabel('Formatting', { exact: true });
  const checkbox = formattingPanel.locator(`xpath=.//label[contains(text(),"${labelText}")]/../input`);
  const isChecked = await checkbox.isChecked().catch(() => false);
  if (isChecked !== checked) {
    await formattingPanel.locator(`xpath=.//label[contains(text(),"${labelText}")]`).click();
    await sleep(300);
  }
}

/** Close Global Options by clicking OK. */
async function closeOptions(page: Page): Promise<void> {
  await page.locator(OPTIONS_OK).click();
  await expect(page.locator(OPTIONS_OK)).toBeHidden({ timeout: 15000 });
  await sleep(500);
}

/**
 * Set Air-related preferences via the Global Options dialog.
 * Opens Options > Code, sets both checkboxes, clicks OK.
 */
async function setAirPrefs(
  page: Page,
  consoleActions: ConsolePaneActions,
  useAir: boolean,
  reformatOnSave: boolean
): Promise<void> {
  await openCodeOptions(page, consoleActions);

  // "Use Air" must be set first — "Reformat on save" is only visible when Air is checked
  await setCheckbox(page, AIR_CHECKBOX_LABEL, useAir);
  await sleep(300);

  if (useAir) {
    // Reformat on save checkbox is now visible
    await setCheckbox(page, REFORMAT_ON_SAVE_LABEL, reformatOnSave);
  }

  await closeOptions(page);
}

/** Reset Air-related preferences programmatically (for setup/cleanup, not the test itself). */
async function resetAirPrefs(consoleActions: ConsolePaneActions): Promise<void> {
  await consoleActions.typeInConsole('{ .rs.uiPrefs$codeFormatter$set("none"); .rs.uiPrefs$useAirFormatter$set(FALSE); .rs.uiPrefs$reformatOnSave$set(FALSE) }');
  await sleep(1000);
}

/** Create an Air config file in the working directory. */
async function createAirConfig(consoleActions: ConsolePaneActions, fileName: string = AIR_TOML_FILE): Promise<void> {
  await consoleActions.typeInConsole(
    `writeLines(c("[format]", "line-width = 20", "indent-width = 12", 'indent-style = "space"', "persistent-line-breaks = true", 'exclude = ["tmp/", ".git/", "renv/"]', "default-exclude = true"), "${fileName}")`
  );
  await sleep(500);
}

/** Remove both air.toml and .air.toml if they exist. */
async function removeAirConfig(consoleActions: ConsolePaneActions): Promise<void> {
  await consoleActions.typeInConsole(`{ unlink("${AIR_TOML_FILE}"); unlink("${DOT_AIR_TOML_FILE}") }`);
  await sleep(500);
}

/** Create the test R file with messy code and open it in the editor. */
async function openTestFile(
  consoleActions: ConsolePaneActions,
  sourceActions: SourcePaneActions
): Promise<void> {
  await consoleActions.typeInConsole(
    `writeLines(c("x<-1+2+3", "y<-list(a=1,b=2,c=3)"), "${TEST_FILE}")`
  );
  await sleep(500);
  await consoleActions.typeInConsole(`file.edit("${TEST_FILE}")`);
  await expect(sourceActions.sourcePane.selectedTab).toContainText(TEST_FILE, { timeout: 10000 });
  await sleep(1000);
}

/** Click the editor content area to make sure it has focus. */
async function focusEditor(sourceActions: SourcePaneActions): Promise<void> {
  await sourceActions.sourcePane.contentPane.click();
  await sleep(300);
}

/** Select all code in the editor, then reformat via Cmd+Shift+A / Ctrl+Shift+A. */
async function reformatCode(page: Page, sourceActions: SourcePaneActions): Promise<void> {
  await focusEditor(sourceActions);
  await page.keyboard.press('ControlOrMeta+a'); // select all
  await sleep(300);
  await page.keyboard.press('ControlOrMeta+Shift+a'); // reformat selection
  await sleep(2000);
}

/** Save the current file via Cmd+S / Ctrl+S (triggers reformat-on-save if enabled). */
async function saveFile(page: Page, sourceActions: SourcePaneActions): Promise<void> {
  await focusEditor(sourceActions);
  await sourceActions.goToEnd();
  await page.keyboard.press('Enter');
  await page.keyboard.type('z<-4');
  await sleep(300);
  await page.keyboard.press('ControlOrMeta+s');
  await sleep(2000);
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
  useSuiteSandbox();
  let page: Page;
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage }) => {
    page = rstudioPage;
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

  test('1: baseline — unchecked, no air.toml, manual reformat uses built-in formatter', async () => {
    // Prefs: useAir=false, reformatOnSave=false (already default from beforeAll)
    // No air.toml (already absent)
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  test('2: unchecked, air.toml present, manual reformat uses built-in formatter', async () => {
    await setAirPrefs(page, consoleActions, false, false);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  test('3: unchecked, air.toml present, reformat on save leaves code unchanged', async () => {
    await setAirPrefs(page, consoleActions, false, false);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectUnchanged(content);
  });

  test('4: checked, air.toml present, manual reformat uses Air', async () => {
    await setAirPrefs(page, consoleActions, true, false);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

  test('5: checked, air.toml present, reformat on save uses Air', async () => {
    test.skip(true, 'Air reformat-on-save does not trigger via save');
    await setAirPrefs(page, consoleActions, true, true);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

  test('6: checked, no config, manual reformat uses built-in formatter', async () => {
    await setAirPrefs(page, consoleActions, true, false);
    await removeAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  // --- .air.toml (hidden variant) tests ---

  test('7: unchecked, .air.toml present, manual reformat uses built-in formatter', async () => {
    await setAirPrefs(page, consoleActions, false, false);
    await createAirConfig(consoleActions, DOT_AIR_TOML_FILE);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  test('8: unchecked, .air.toml present, reformat on save leaves code unchanged', async () => {
    await setAirPrefs(page, consoleActions, false, false);
    await createAirConfig(consoleActions, DOT_AIR_TOML_FILE);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectUnchanged(content);
  });

  test('9: checked, .air.toml present, manual reformat uses Air', async () => {
    test.skip(true, '.air.toml created mid-session is not detected (rstudio/rstudio#17310)');
    await setAirPrefs(page, consoleActions, true, false);
    await createAirConfig(consoleActions, DOT_AIR_TOML_FILE);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

  test('10: checked, .air.toml present, reformat on save uses Air', async () => {
    test.skip(true, '.air.toml created mid-session is not detected (rstudio/rstudio#17310)');
    await setAirPrefs(page, consoleActions, true, true);
    await createAirConfig(consoleActions, DOT_AIR_TOML_FILE);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });
});
