import { test, expect } from '@fixtures/rstudio.fixture';
import { executeCommand, dismissAllModals, numModalsShowing } from '@utils/commands';
import { createAndOpenProject, closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import type { Locator, Page } from 'playwright';

// When a project sets its own editor theme, the global Appearance pane's theme
// selector is ignored. The pane (AppearancePreferencesPane) hides the selector
// and Add/Remove buttons and shows an indicator with an "Edit Project
// Options..." button, previewing the effective (project) theme instead of the
// global one. Clearing the project theme reverts the pane live via
// ProjectOptionsChangedEvent. These tests exercise both the construction-time
// state and the live revert path through the in-pane button.

// -- Selectors ----------------------------------------------------------------

// PreferencesDialogBase.setText(caption) sets this aria-label on the
// gwt-DialogBox; the global Options caption is "Options", the project one is
// "Project Options". Both dialogs can be open at once (the global pane's button
// opens the project one on top), so interactions are scoped to a container.
const GLOBAL_OPTIONS_DIALOG = '.gwt-DialogBox[aria-label="Options"]';
const PROJECT_OPTIONS_DIALOG = '.gwt-DialogBox[aria-label="Project Options"]';

// SectionChooser assigns the tab id "rstudio_label_<label>_options". Both
// dialogs have an "Appearance" pane, so this id is shared -- always scope it.
const APPEARANCE_TAB = '#rstudio_label_appearance_options';

// ElementIds.assignElementId bases (prefixed with "rstudio_").
const GLOBAL_THEME_SELECT = '#rstudio_appearance_editor_theme';
const GLOBAL_THEME_OVERRIDE = '#rstudio_appearance_editor_theme_project_override';
const PROJECT_THEME_SELECT = '#rstudio_project_editor_theme';

// ModalDialogBase.addButton assigns these via ElementIds.assignElementId, which
// disambiguates with a numeric suffix when another dialog already holds the base
// id -- the project dialog opens on top of the global one, so its OK button is
// "rstudio_preferences_confirm_0", not the plain id. Match by id prefix, scoped
// to a dialog container, so either form resolves to the one button per dialog.
const PREFERENCES_OK = '[id^="rstudio_preferences_confirm"]';
const DIALOG_CANCEL = '[id^="rstudio_dlg_cancel"]';

const PROJECT_OVERRIDE_TEXT = 'The editor theme is overridden by project settings.';
const EDIT_PROJECT_OPTIONS_BUTTON = 'Edit Project Options...';
const DEFAULT_OPTION_LABEL = '(Default)';
const PROJECT_THEME = 'Cobalt';

// -- Helpers ------------------------------------------------------------------

/** Open the project Appearance tab and select a theme, then save and wait for close. */
async function setProjectEditorTheme(page: Page, themeLabel: string): Promise<void> {
  await executeCommand(page, 'projectOptions');
  const dialog = page.locator(PROJECT_OPTIONS_DIALOG);
  await expect(dialog).toBeVisible({ timeout: 15000 });
  await dialog.locator(APPEARANCE_TAB).click();

  const select = dialog.locator(PROJECT_THEME_SELECT);
  await expect(select).toBeVisible({ timeout: 5000 });
  // selectOption would throw if the requested label weren't an option yet, so
  // wait for the async theme list to land first.
  await expect(select.locator('option', { hasText: themeLabel })).toHaveCount(1, {
    timeout: 15000,
  });

  await select.selectOption({ label: themeLabel });
  // onApply reads the DOM <select>; fire change explicitly so the pane observes
  // the selection regardless of how selectOption dispatched it.
  await select.dispatchEvent('change');

  await dialog.locator(PREFERENCES_OK).click();
  await expect(dialog).not.toBeVisible({ timeout: 10000 });
}

/** Open global Options and switch to its Appearance tab. */
async function openGlobalAppearance(page: Page): Promise<Locator> {
  await executeCommand(page, 'showOptions');
  const dialog = page.locator(GLOBAL_OPTIONS_DIALOG);
  await expect(dialog).toBeVisible({ timeout: 15000 });
  await dialog.locator(APPEARANCE_TAB).click();
  return dialog;
}

/** Cancel out of a still-open dialog without persisting changes. */
async function cancelDialog(page: Page, dialogSelector: string): Promise<void> {
  const dialog = page.locator(dialogSelector);
  await dialog.locator(DIALOG_CANCEL).click();
  await expect(dialog).not.toBeVisible({ timeout: 10000 });
}

// -- Test suite ---------------------------------------------------------------

test.describe.serial('Global Appearance pane project theme override', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    // The override indicator only shows inside a project, and the in-pane
    // button (projectOptions command) is only enabled with a project open.
    await createAndOpenProject(page, sandbox.dir, 'GlobalOverride');
    // Seed a project editor theme so the global pane has an override to show.
    await setProjectEditorTheme(page, PROJECT_THEME);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      if ((await numModalsShowing(page)) > 0) {
        await dismissAllModals(page);
      }
    } catch (err) {
      console.warn('[global_appearance_project_override] afterAll dismissAllModals failed:', err);
    }
    // Closing the project restarts the session, dropping the project pref layer
    // and restoring the global editor theme. The sandbox is removed by teardown.
    try {
      await closeProjectIfOpen(page);
    } catch (err) {
      console.warn('[global_appearance_project_override] afterAll closeProjectIfOpen failed:', err);
    }
  });

  test('shows the override indicator and hides the theme selector', async ({
    rstudioPage: page,
  }) => {
    const dialog = await openGlobalAppearance(page);

    // The override panel appears after the async theme list loads; the panel
    // and the selector toggle together, so wait for the panel first.
    const overridePanel = dialog.locator(GLOBAL_THEME_OVERRIDE);
    await expect(overridePanel).toBeVisible({ timeout: 15000 });
    await expect(overridePanel).toContainText(PROJECT_OVERRIDE_TEXT);
    await expect(
      overridePanel.getByText(EDIT_PROJECT_OPTIONS_BUTTON, { exact: true }),
    ).toBeVisible();

    // With the override active, the normal theme selector is hidden.
    await expect(dialog.locator(GLOBAL_THEME_SELECT)).not.toBeVisible();

    await cancelDialog(page, GLOBAL_OPTIONS_DIALOG);
  });

  test('reverts live when the project theme is cleared via the button', async ({
    rstudioPage: page,
  }) => {
    const globalDialog = await openGlobalAppearance(page);

    const overridePanel = globalDialog.locator(GLOBAL_THEME_OVERRIDE);
    await expect(overridePanel).toBeVisible({ timeout: 15000 });

    // Launch project options from the in-pane button (opens on top of global).
    await overridePanel.getByText(EDIT_PROJECT_OPTIONS_BUTTON, { exact: true }).click();
    const projectDialog = page.locator(PROJECT_OPTIONS_DIALOG);
    await expect(projectDialog).toBeVisible({ timeout: 15000 });

    // Clear the project theme back to (Default) and save.
    await projectDialog.locator(APPEARANCE_TAB).click();
    const projectSelect = projectDialog.locator(PROJECT_THEME_SELECT);
    await expect(projectSelect).toBeVisible({ timeout: 5000 });
    await projectSelect.selectOption({ label: DEFAULT_OPTION_LABEL });
    await projectSelect.dispatchEvent('change');
    await projectDialog.locator(PREFERENCES_OK).click();
    await expect(projectDialog).not.toBeVisible({ timeout: 10000 });

    // The global pane handles ProjectOptionsChangedEvent: the indicator hides
    // and the normal theme selector returns, all without reopening the dialog.
    await expect(overridePanel).not.toBeVisible({ timeout: 10000 });
    await expect(globalDialog.locator(GLOBAL_THEME_SELECT)).toBeVisible({ timeout: 10000 });

    // Safety net: if a regression in AssistantPreferencesPane lets a project-
    // options save fire a spurious "Install Posit Assistant" modal (the global
    // assistant pref defaults to "posit"), it would sit on top of the global
    // dialog and intercept the cancel click. The live-revert assertions above
    // are the real subject; clear any stray modal so it can't wedge teardown.
    if ((await numModalsShowing(page)) > 1) {
      await dismissAllModals(page);
      await expect(globalDialog).not.toBeVisible({ timeout: 10000 });
      return;
    }

    await cancelDialog(page, GLOBAL_OPTIONS_DIALOG);
  });
});
