import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand, dismissAllModals, numModalsShowing } from '@utils/commands';
import { createAndOpenProject, closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';

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

// The placeholder option the pane always seeds first.
const DEFAULT_OPTION_LABEL = '(Default)';

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
});
