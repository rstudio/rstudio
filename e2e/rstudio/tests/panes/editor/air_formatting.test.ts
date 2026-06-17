/**
 * Air Formatting (#16721)
 *
 * Verifies that the Air formatter respects the "Use Air" checkbox.
 * Bug: Air formatting was always used if air.toml was found, even if
 * the global option was turned off.
 *
 * Fix: PR #17202 — requires both conditions for Air:
 *   1. Formatter set to "none" (default)
 *   2. Air checkbox enabled
 *
 * Since #17748, an air.toml is no longer required by default: with "Use Air"
 * checked and no config present, the backend synthesizes an air.toml from the
 * user's editor indentation settings (indent-style, indent-width), so Air
 * matches the editor. With default editor settings this is identical to Air's
 * own defaults. The old air.toml gate is available behind the opt-in "Only
 * use Air when an air.toml file is found" checkbox.
 *
 * RStudio checks for both air.toml and .air.toml (hidden variant).
 *
 * Test matrix:
 *   1.  unchecked + no config    + manual reformat  → built-in formatter
 *   2.  unchecked + air.toml     + manual reformat  → built-in formatter
 *   3.  unchecked + air.toml     + save              → code unchanged
 *   4.  checked   + air.toml     + manual reformat  → Air formatting
 *   5.  checked   + air.toml     + save              → Air formatting
 *   6.  checked   + no config    + save              → Air (default settings)
 *   7.  unchecked + .air.toml    + manual reformat  → built-in formatter
 *   8.  unchecked + .air.toml    + save              → code unchanged
 *   9.  checked + require-toml + no config + manual reformat → built-in
 *   10. checked + require-toml + air.toml  + manual reformat → Air formatting
 *   11. checked + require-toml + no config + save             → code unchanged
 *   12. checked + no config + editor indent width 4 + save    → Air honors
 *       the synthesized indent-width
 *   13. external "air format" + air.toml + save               → Air honors the
 *       project air.toml (#18003)
 *
 * Detection: air.toml uses 12-space indent — unmistakable signal. Air with
 * default settings (case 6) is exercised via the save path, where the
 * built-in formatter never runs (maybeFormatOnUserInitiatedSave skips it),
 * so any formatting change on save proves Air ran.
 */

import { expect } from '@playwright/test';
import type { Page } from 'playwright';
import { test } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand, saveDocument, setPref } from '@utils/commands';
import { openFile } from '@utils/files';
import { closeProjectIfOpen, createAndOpenProject } from '@utils/project';

// --- Test data (from issue #16721) ---

const TEST_FILE = `air_format_test_${Date.now()}.R`;
const AIR_TOML_FILE = 'air.toml'; // RStudio checks for both air.toml and .air.toml
const DOT_AIR_TOML_FILE = '.air.toml';

// Checkbox labels as they appear in Global Options > Code
// Source: UserPrefsAccessorConstants_en.properties
const AIR_CHECKBOX_LABEL = 'Use Air for code formatting';
const REFORMAT_ON_SAVE_LABEL = 'Reformat documents on save';
const AIR_REQUIRE_TOML_LABEL = 'Only use Air when an air.toml file is found';

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
  reformatOnSave: boolean,
  requireToml: boolean = false
): Promise<void> {
  await openCodeOptions(page);

  // "Use Air" must be set first -- the other two checkboxes are only visible
  // when Air is checked. setCheckbox already polls for the click to land, so
  // no extra wait needed.
  await setCheckbox(page, AIR_CHECKBOX_LABEL, useAir);

  if (useAir) {
    // The dependent checkboxes are now visible
    await setCheckbox(page, REFORMAT_ON_SAVE_LABEL, reformatOnSave);
    await setCheckbox(page, AIR_REQUIRE_TOML_LABEL, requireToml);
  }

  await closeOptions(page);
}

/** Reset Air-related preferences programmatically (for setup/cleanup, not the test itself). */
async function resetAirPrefs(consoleActions: ConsolePaneActions): Promise<void> {
  await setPref(consoleActions.page, 'code_formatter', 'none');
  await setPref(consoleActions.page, 'use_air_formatter', false);
  await setPref(consoleActions.page, 'air_formatter_require_toml', false);
  await setPref(consoleActions.page, 'reformat_on_save', false);
  await setPref(consoleActions.page, 'code_formatter_external_command', '');
}

/**
 * Resolve the path to the Air binary, mirroring the reformat path's own lookup
 * (.rs.air.ensureAvailable). The marker is split across two string literals so
 * the echoed command line can't false-match -- only the assembled output line
 * contains "AIR_PATH=". Returns null if the path couldn't be read.
 */
async function readAirPath(consoleActions: ConsolePaneActions): Promise<string | null> {
  await consoleActions.clearConsole();
  await consoleActions.executeInConsole(
    'cat(paste0("AIR", "_PATH=", .rs.air.ensureAvailable()), "\\n")',
    { wait: true },
  );
  const output = await consoleActions.consolePane.consoleOutput.innerText();
  const match = output.match(/AIR_PATH=([^\n]+)/);
  return match ? match[1].trim() : null;
}

/**
 * Wait for the GWT-side cached air.toml path (maintained from FileChangeEvents
 * on Projects) to reflect the on-disk state. The formatter decision consults
 * this cache, and the file monitor surfaces changes asynchronously -- without
 * this wait, a save right after creating/deleting an air.toml can race the
 * DELETE/ADD event and use the stale cached value.
 */
async function waitForAirTomlCache(page: Page, present: boolean): Promise<void> {
  await expect
    .poll(
      async () => {
        const path = await page.evaluate(() => window.rstudio?.project?.airTomlPath() ?? null);
        return path !== null;
      },
      { timeout: 10000 },
    )
    .toBe(present);
}

/** Create an Air config file in the working directory. */
async function createAirConfig(consoleActions: ConsolePaneActions, fileName: string = AIR_TOML_FILE): Promise<void> {
  await consoleActions.executeInConsole(
    `writeLines(c("[format]", "line-width = 20", "indent-width = 12", 'indent-style = "space"', "persistent-line-breaks = true", 'exclude = ["tmp/", ".git/", "renv/"]', "default-exclude = true"), "${fileName}")`,
    { wait: true },
  );
  // The file monitor does not emit FileChangeEvents for hidden files, so the
  // GWT cache never learns about .air.toml -- only wait for the visible
  // variant. The hidden variant is still honored at format time through the
  // format_context RPC, which consults the filesystem directly.
  if (fileName === AIR_TOML_FILE) {
    await waitForAirTomlCache(consoleActions.page, true);
  }
}

/** Remove both air.toml and .air.toml if they exist. */
async function removeAirConfig(consoleActions: ConsolePaneActions): Promise<void> {
  await consoleActions.executeInConsole(
    `{ unlink("${AIR_TOML_FILE}"); unlink("${DOT_AIR_TOML_FILE}") }`,
    { wait: true },
  );
  await waitForAirTomlCache(consoleActions.page, false);
}

/** Create the test R file with messy code and open it in the editor. */
async function openTestFile(
  consoleActions: ConsolePaneActions,
  sourceActions: SourcePaneActions,
  lines: string[] = ['x<-1+2+3', 'y<-list(a=1,b=2,c=3)']
): Promise<void> {
  const quoted = lines.map((line) => `"${line}"`).join(', ');
  await consoleActions.executeInConsole(
    `writeLines(c(${quoted}), "${TEST_FILE}")`,
    { wait: true },
  );
  // openFile waits for the source-pane Ace instance to be reachable, not
  // just for the tab title to render. Without that wait, downstream
  // getEditorContent() can read the editor before its document body has
  // loaded and the failure surfaces as a confusing reformatCode timeout
  // instead of "file never finished loading."
  await openFile(sourceActions.page, TEST_FILE);
}

/** Click the editor content area to make sure it has focus. */
async function focusEditor(sourceActions: SourcePaneActions): Promise<void> {
  await sourceActions.sourcePane.contentPane.click();
  // Wait until focus has actually landed on a source-pane Ace textarea --
  // click() resolves before the focus event has propagated.
  try {
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
  } catch (err) {
    // The bare waitForFunction timeout reads as an opaque "Timeout exceeded
    // while waiting on the predicate" -- which makes it impossible to tell
    // whether focus drifted to the console, the source pane has no editor
    // at all (file load failed), or something stranger. Capture the actual
    // active-element state and re-raise.
    const diag = await sourceActions.page.evaluate(() => {
      const el = document.activeElement as HTMLElement | null;
      const describe = (e: Element | null): string => {
        if (!e) return 'null';
        const tag = e.tagName.toLowerCase();
        const id = e.id ? `#${e.id}` : '';
        const className = typeof (e as HTMLElement).className === 'string'
          ? (e as HTMLElement).className
          : '';
        const cls = className
          ? '.' + className.split(/\s+/).filter(Boolean).join('.')
          : '';
        return `${tag}${id}${cls}`;
      };
      const sourceAceCount = document.querySelectorAll(
        "[class*='rstudio_source_panel'] textarea.ace_text-input",
      ).length;
      return {
        active: describe(el),
        inConsole: !!(el && el.closest('#rstudio_console_input')),
        inSourcePanel: !!(el && el.closest("[class*='rstudio_source_panel']")),
        sourceAceCount,
      };
    });
    throw new Error(
      `focusEditor: focus did not land on a source-pane ace_text-input ` +
      `(active=${diag.active}, inConsole=${diag.inConsole}, ` +
      `inSourcePanel=${diag.inSourcePanel}, sourceAceCount=${diag.sourceAceCount}). ` +
      `Original: ${(err as Error).message}`,
    );
  }
}

/** Select all code in the editor, then dispatch the reformat command. */
async function reformatCode(page: Page, sourceActions: SourcePaneActions): Promise<void> {
  // Snapshot the content before the command so we can poll for the diff
  // to land. The reformat command (whether server-side Air or client-side
  // built-in) applies its diff asynchronously and exposes no per-command
  // signal; every caller of this helper expects the content to *change*,
  // so a content-change poll is a precise readiness signal. Callers that
  // need to assert "no change" should use saveFile (which polls dirty=false
  // via saveDocument) instead.
  //
  // Drive select-all and reformat through the window.rstudio bridge rather
  // than Cmd+A / Cmd+Shift+A keyboard presses. The keyboard path lost the
  // race on slow CI: focus could drift off the source pane between the two
  // keystrokes (or between the keystroke and the command dispatch), at
  // which point Cmd+Shift+A landed on a different focus target and the
  // reformat never fired -- the failure surfaced as a 10s opaque timeout
  // because *nothing happened* to the buffer.
  const before = await sourceActions.getEditorContent();
  await page.evaluate(() => {
    const editor = window.rstudio?.documents.activeEditor();
    if (!editor) throw new Error('reformatCode: no active source editor');
    editor.selectAll();
  });
  await executeCommand(page, 'reformatCode');
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

/**
 * Air formatted without an air.toml: the suite's 12-indent config is absent
 * and the synthesized config carries the default editor settings, so the
 * output looks like ordinary tidied code. Callers must only use this on the
 * save path, where the built-in formatter cannot run -- there, any formatting
 * change proves Air ran.
 */
function expectAirDefaultFormatted(content: string): void {
  expect(content).toContain('x <- 1 + 2 + 3');
  expect(content).toContain('y <- list(a = 1, b = 2, c = 3)');
  expect(content).not.toContain('            '); // no 12 spaces
}

/**
 * Built-in formatted: spaces added around operators, no 12-space indent.
 * Note the built-in formatter and Air-with-default-settings produce identical
 * output for this input, so this assertion cannot distinguish them; the
 * save-path cases (where the built-in formatter never runs) are the tests
 * that pin down *which* formatter ran.
 */
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
  // Whether the Air binary can be resolved on this machine. Computed once in
  // beforeAll; the cases that enable "Use Air" (4/5/6/9/10/11) skip when it's
  // false so a runner without Air doesn't report failures for an unavailable
  // dependency. The useAir=false cases (1/2/3/7/8) need no Air and always
  // run -- they are the core #16721 regression coverage.
  let airAvailable = false;

  test.beforeAll(async ({ rstudioPage }) => {
    page = rstudioPage;
    // The save-triggered reformat path in TextEditingTarget.maybeFormatOnUserInitiatedSave
    // is gated on "file is inside the active project" -- without a project,
    // reformat-on-save is skipped entirely, so the save-path cases (5/6/11)
    // can't pass. Open a
    // project inside the suite sandbox; the project open also re-`setwd`s into
    // the project dir, so relative paths still resolve consistently and air.toml
    // ends up alongside the file being formatted (which is what Air's ancestor
    // walk needs to find it).
    //
    // The project declares a 4-space indent: with a project open the project
    // layer supplies use_spaces_for_tab/num_spaces_for_tab unconditionally
    // (ProjectContext::uiPrefs), so a user-level setPref can't reach the
    // backend, and the config isn't reloaded if the .Rproj changes after
    // open. Case 12 relies on this width to detect the synthesized air.toml;
    // the other cases' inputs produce no indented lines, so it can't affect
    // them.
    await createAndOpenProject(page, sandbox.dir, 'air_formatting', [
      'UseSpacesForTab: Yes',
      'NumSpacesForTab: 4',
    ]);
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
    await consoleActions.clearConsole();
    // Start clean — programmatic reset (not the thing being tested)
    await removeAirConfig(consoleActions);
    await resetAirPrefs(consoleActions);

    // Smoke probe: exercise the reformat machinery once with the simplest
    // configuration (built-in formatter, no Air, no config). If the command
    // bridge, source pane, or built-in formatter is wedged, every test below
    // would fail with a 10-second opaque "content never changed" timeout --
    // surface that here as a single beforeAll failure with the actual buffer
    // we saw, so the cause is recognizable instead of a wall of identical
    // reformatCode timeouts.
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const probeContent = await sourceActions.getEditorContent();
    if (!probeContent.includes('x <- 1 + 2 + 3')) {
      throw new Error(
        'Air suite smoke probe failed: built-in reformat did not produce ' +
        `expected output. Got:\n${probeContent}`,
      );
    }
    await sourceActions.closeSourceAndDeleteFile(TEST_FILE);

    // Preflight: can Air be resolved on this machine? .rs.air.ensureAvailable()
    // returns a path to the Air binary or stop()s if it can't find (or, by
    // default, install) one. This mirrors exactly what the reformat path does
    // -- including autoinstall, so a sandbox where Air isn't on PATH but is
    // installable still reports available -- and treats any error as "not
    // available". The Air-dependent cases (4/5/6/9/10/11) skip when this is
    // false rather than failing on a dependency the runner genuinely can't
    // provide.
    airAvailable =
      (await consoleActions.evalRLogical(
        'tryCatch(isTRUE(file.exists(.rs.air.ensureAvailable())), error = function(e) FALSE)',
      )) === true;
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
    test.skip(!airAvailable, 'Air binary not available (.rs.air.ensureAvailable could not resolve it)');
    await setAirPrefs(page, true, false);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

  test('5: checked, air.toml present, save uses Air', async () => {
    test.skip(!airAvailable, 'Air binary not available (.rs.air.ensureAvailable could not resolve it)');
    await setAirPrefs(page, true, true);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

  test('6: checked, no config, save uses Air with default settings', async () => {
    // #17748: no air.toml is required by default -- Air formats using a
    // synthesized config carrying the editor settings (here, the defaults).
    // Exercised via save, where the built-in formatter never runs, so the
    // formatting change below can only come from Air.
    test.skip(!airAvailable, 'Air binary not available (.rs.air.ensureAvailable could not resolve it)');
    await setAirPrefs(page, true, true);
    await removeAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirDefaultFormatted(content);
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

  // --- "Only use Air when an air.toml file is found" (#17748) ---

  test('9: checked + require-toml, no config, manual reformat uses built-in formatter', async () => {
    test.skip(!airAvailable, 'Air binary not available (.rs.air.ensureAvailable could not resolve it)');
    await setAirPrefs(page, true, false, true);
    await removeAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectBuiltinFormatted(content);
  });

  test('10: checked + require-toml, air.toml present, manual reformat uses Air', async () => {
    test.skip(!airAvailable, 'Air binary not available (.rs.air.ensureAvailable could not resolve it)');
    await setAirPrefs(page, true, false, true);
    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await reformatCode(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

  test('11: checked + require-toml, no config, save leaves code unchanged', async () => {
    // The exact scenario from #17748, under the new opt-in gate: Air is
    // enabled but gated on an air.toml that does not exist, and the built-in
    // formatter never runs on save -- so saving changes nothing.
    test.skip(!airAvailable, 'Air binary not available (.rs.air.ensureAvailable could not resolve it)');
    await setAirPrefs(page, true, true, true);
    await removeAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectUnchanged(content);
  });

  // --- air.toml synthesis from editor settings (#17748) ---

  test('12: checked, no config, save honors the editor indent width via synthesized air.toml', async () => {
    // With no air.toml on disk, the backend synthesizes one carrying the
    // effective editor indentation settings -- here the suite project's
    // NumSpacesForTab: 4 (see beforeAll). The width is the discriminator:
    // Air's own default is 2, so a 4-space function body proves the
    // synthesized config was discovered -- not Air defaults, and not the
    // built-in formatter (which never runs on the save path).
    test.skip(!airAvailable, 'Air binary not available (.rs.air.ensureAvailable could not resolve it)');
    await setAirPrefs(page, true, true);
    await removeAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions, ['f<-function(){', '1+2', '}']);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expect(content).toContain('f <- function() {');
    expect(content).toContain('\n    1 + 2\n'); // 4-space indent from editor pref
  });

  // --- External "air format" command respects project air.toml (#18003) ---

  test('13: external "air format" command, air.toml present, save respects air.toml', async () => {
    // The configuration the Air documentation recommends for format-on-save:
    // Code formatter = External, Reformat command = "air format". RStudio
    // formats a copy of the document in a temporary directory, and Air resolves
    // its configuration relative to the file being formatted -- so the project
    // air.toml must be copied next to that temp file, or Air falls back to its
    // 2-space default (#18003). The suite air.toml uses a 12-space indent,
    // which is neither Air's default (2) nor the project pref (4), so a 12-space
    // indent proves the project air.toml was honored.
    test.skip(!airAvailable, 'Air binary not available (.rs.air.ensureAvailable could not resolve it)');

    const airPath = await readAirPath(consoleActions);
    expect(airPath).not.toBeNull();
    await setPref(page, 'code_formatter', 'external');
    await setPref(page, 'code_formatter_external_command', `"${airPath}" format`);
    await setPref(page, 'reformat_on_save', true);

    await createAirConfig(consoleActions);
    await openTestFile(consoleActions, sourceActions);
    await saveFile(page, sourceActions);
    const content = await sourceActions.getEditorContent();
    expectAirFormatted(content);
  });

});
