import { test, expect } from '@fixtures/rstudio.fixture';
import {
  executeInConsole,
  waitForConsoleIdle,
  CONSOLE_OUTPUT,
} from '@pages/console_pane.page';
import { executeCommand, dismissAllModals, numModalsShowing } from '@utils/commands';
import { createAndOpenProject, closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral } from '@utils/r';
import type { Page } from 'playwright';

// Regression for the project Appearance pane editor-theme dropdown.
//
// The pane (ProjectAppearancePreferencesPane) loads installed themes via
// AceThemes.getThemes(), whose DelayedProgressRequestCallback dereferences the
// ProgressIndicator in onResponseReceived(). Passing a null indicator there
// NPE'd before onSuccess() ran, so the dropdown was left with only the
// "(Default)" placeholder and never populated with installed themes. The fix
// passes getProgressIndicator() (non-null once the dialog sets it on each
// pane). This test exercises the dialog UI path -- distinct from the
// project-open path covered by project_editor_theme.test.ts -- and asserts the
// dropdown ends up with MORE THAN ONE option (placeholder + installed themes).

// -- Selectors ----------------------------------------------------------------

// PreferencesDialogBase.setText(caption) sets this aria-label on the gwt-DialogBox.
const PROJECT_OPTIONS_DIALOG = '.gwt-DialogBox[aria-label="Project Options"]';

// SectionChooser.addSection assigns the tab id "rstudio_label_<label>_options"
// (ElementIds.idFromLabel + "_options"). The Appearance pane's name is "Appearance".
const APPEARANCE_TAB = '#rstudio_label_appearance_options';

// ProjectAppearancePreferencesPane assigns this id to its editor-theme ListBox,
// which renders as a multi-row <select> (a listbox, not a dropdown).
const THEME_SELECT = '#rstudio_project_editor_theme';

// PreferencesDialogBase.addCancelButton() assigns this stable id; using it
// avoids matching the hidden aria-hidden duplicate the dialog also renders.
const PROJECT_OPTIONS_CANCEL = '#rstudio_dlg_cancel';

// PreferencesDialogBase.addOkButton(okButton, ElementIds.PREFERENCES_CONFIRM)
// with PREFERENCES_CONFIRM = "preferences_confirm" -> "rstudio_" prefixed id.
const PROJECT_OPTIONS_OK = '#rstudio_preferences_confirm';

// The placeholder option the pane always seeds first.
const DEFAULT_OPTION_LABEL = '(Default)';

/**
 * Read the contents of the project's .Rproj via the R console and return the
 * text between fresh unique delimiters. Reading R-side (not the runner's FS)
 * keeps this working in Server mode where the rsession host may not share the
 * test runner's filesystem. A unique token per call avoids parsing a stale
 * dump left in the accumulated console output.
 */
async function readRprojViaConsole(page: Page, rprojPath: string): Promise<string> {
  const token = `PWRPROJ_${Date.now()}_${Math.random().toString(36).slice(2)}`;
  const open = `<<<${token}`;
  const close = `${token}>>>`;
  const rprojLit = rPathLiteral(rprojPath);

  await executeInConsole(
    page,
    `cat("${open}\\n", paste(readLines(${rprojLit}), collapse="\\n"), "\\n${close}\\n", sep="")`,
    { wait: true },
  );
  await waitForConsoleIdle(page);

  const output = await page.locator(CONSOLE_OUTPUT).innerText();
  const match = output.match(new RegExp(`${open}\\n([\\s\\S]*?)\\n${close}`));
  return match ? match[1] : '';
}

// -- Test suite ---------------------------------------------------------------

test.describe.serial('Project Appearance pane theme dropdown', () => {
  const sandbox = useSuiteSandbox();
  let projectDir = '';

  test.beforeAll(async ({ rstudioPage: page }) => {
    // projectOptions is only enabled when a project is open, so open one.
    projectDir = await createAndOpenProject(page, sandbox.dir, 'AppearancePane');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // Dismiss any lingering dialog so it can't block the close path, then
    // close the project. The sandbox subtree is removed by global teardown.
    try {
      if ((await numModalsShowing(page)) > 0) {
        await dismissAllModals(page);
      }
    } catch (err) {
      console.warn('[project_appearance_pane] afterAll dismissAllModals failed:', err);
    }
    try {
      await closeProjectIfOpen(page);
    } catch (err) {
      console.warn('[project_appearance_pane] afterAll closeProjectIfOpen failed:', err);
    }
  });

  test('editor-theme dropdown populates with installed themes', async ({
    rstudioPage: page,
  }) => {
    // Open Project Options via the AppCommand bridge.
    await executeCommand(page, 'projectOptions');
    await expect(page.locator(PROJECT_OPTIONS_DIALOG)).toBeVisible({ timeout: 15000 });

    // PreferencesDialogBase.initialize() initializes ALL panes when the dialog
    // opens, so the Appearance dropdown populates even if its tab isn't active.
    // Click the tab anyway so the <select> is rendered/visible for reading.
    await page.locator(APPEARANCE_TAB).click();

    const select = page.locator(THEME_SELECT);
    await expect(select).toBeVisible({ timeout: 5000 });

    // The themes arrive asynchronously after the get_themes RPC. Poll the
    // option count until the installed themes land (more than the lone
    // "(Default)" placeholder). The null-indicator bug left exactly one option.
    await expect
      .poll(
        () => select.locator('option').count(),
        {
          message: 'Expected editor-theme dropdown to populate with installed themes (> 1 option)',
          timeout: 15000,
          intervals: [200, 500, 1000],
        },
      )
      .toBeGreaterThan(1);

    // The placeholder remains the first option, for clarity.
    await expect(select.locator('option').first()).toHaveText(DEFAULT_OPTION_LABEL);

    // A known default theme (Cobalt ships with RStudio) is among the options,
    // confirming the list is real installed themes, not just the placeholder.
    await expect(select.locator('option', { hasText: 'Cobalt' })).toHaveCount(1);

    // Dismiss without saving so no project config is written.
    await page.locator(PROJECT_OPTIONS_CANCEL).click();
    await expect(page.locator(PROJECT_OPTIONS_DIALOG)).not.toBeVisible({ timeout: 10000 });
  });

  test('saving the pane writes then erases EditorTheme in .Rproj', async ({
    rstudioPage: page,
  }) => {
    const rprojPath = `${projectDir.replace(/\\/g, '/')}/AppearancePane.Rproj`;

    // --- Select "Cobalt" and save: .Rproj should gain "EditorTheme: Cobalt" ---
    await executeCommand(page, 'projectOptions');
    await expect(page.locator(PROJECT_OPTIONS_DIALOG)).toBeVisible({ timeout: 15000 });
    await page.locator(APPEARANCE_TAB).click();

    const select = page.locator(THEME_SELECT);
    await expect(select).toBeVisible({ timeout: 5000 });
    // Wait for the installed themes to land before selecting; selectOption
    // would throw if "Cobalt" weren't an option yet.
    await expect(select.locator('option', { hasText: 'Cobalt' })).toHaveCount(1, { timeout: 15000 });

    await select.selectOption({ label: 'Cobalt' });
    // onApply reads the DOM <select> via ListBox.getSelectedValue(), and the
    // pane's change handler keys off the change event; fire it explicitly so
    // the selection is observed regardless of how selectOption dispatched.
    await select.dispatchEvent('change');

    await page.locator(PROJECT_OPTIONS_OK).click();
    await expect(page.locator(PROJECT_OPTIONS_DIALOG)).not.toBeVisible({ timeout: 10000 });

    // The .Rproj write happens asynchronously after the dialog closes (the
    // writeProjectOptions RPC), so poll the file until it reflects the override.
    await expect
      .poll(() => readRprojViaConsole(page, rprojPath), {
        message: 'Expected .Rproj to gain "EditorTheme: Cobalt" after saving the pane',
        timeout: 15000,
        intervals: [500, 1000],
      })
      .toMatch(/^EditorTheme: Cobalt$/m);

    // --- Select "(Default)" and save: the EditorTheme line should be erased ---
    await executeCommand(page, 'projectOptions');
    await expect(page.locator(PROJECT_OPTIONS_DIALOG)).toBeVisible({ timeout: 15000 });
    await page.locator(APPEARANCE_TAB).click();
    await expect(select).toBeVisible({ timeout: 5000 });

    await select.selectOption({ label: DEFAULT_OPTION_LABEL });
    await select.dispatchEvent('change');

    await page.locator(PROJECT_OPTIONS_OK).click();
    await expect(page.locator(PROJECT_OPTIONS_DIALOG)).not.toBeVisible({ timeout: 10000 });

    // Poll until the EditorTheme line is gone. Also require a stable line
    // (Version: 1.0) to be present, so an empty/failed console read or a
    // truncated .Rproj cannot satisfy the negative assertion.
    await expect
      .poll(
        async () => {
          const content = await readRprojViaConsole(page, rprojPath);
          return content.includes('Version: 1.0') && !/^EditorTheme:/m.test(content);
        },
        {
          message: 'Expected .Rproj to drop the EditorTheme line (and remain intact) after selecting (Default)',
          timeout: 15000,
          intervals: [500, 1000],
        },
      )
      .toBe(true);
  });
});
