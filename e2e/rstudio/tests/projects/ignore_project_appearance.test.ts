import { test, expect } from '@fixtures/rstudio.fixture';
import {
  executeCommand,
  dismissAllModals,
  numModalsShowing,
  getPref,
  setPref,
  clearPref,
} from '@utils/commands';
import { createAndOpenProject, closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import type { Locator, Page } from 'playwright';

// "Ignore project-specific appearance settings" (Global Options > Appearance)
// makes RStudio apply the global editor theme even when the active project sets
// its own. Toggling the checkbox changes nothing until OK/Apply; on Apply the
// pane updates immediately (the project-override indicator gives way to the
// theme selector) and the live editor theme reverts to the global one.

// -- Selectors ----------------------------------------------------------------

const GLOBAL_OPTIONS_DIALOG = '.gwt-DialogBox[aria-label="Options"]';
const PROJECT_OPTIONS_DIALOG = '.gwt-DialogBox[aria-label="Project Options"]';
const APPEARANCE_TAB = '#rstudio_label_appearance_options';

const GLOBAL_THEME_SELECT = '#rstudio_appearance_editor_theme';
const GLOBAL_THEME_OVERRIDE = '#rstudio_appearance_editor_theme_project_override';
const PROJECT_THEME_SELECT = '#rstudio_project_editor_theme';

// ModalDialogBase assigns these via ElementIds.assignElementId, disambiguating
// with a numeric suffix when another dialog already holds the base id (the
// project dialog can open on top of the global one). Match by id prefix.
const PREFERENCES_OK = '[id^="rstudio_preferences_confirm"]';
const PREFERENCES_APPLY = '[id^="rstudio_dlg_apply"]';
const DIALOG_CANCEL = '[id^="rstudio_dlg_cancel"]';

// GWT CheckBox renders an <input type="checkbox"> associated with its label, so
// the checkbox resolves by accessible name (PrefsConstants.ignoreProjectAppearanceLabel).
const IGNORE_CHECKBOX_LABEL = 'Ignore project-specific appearance settings';

// The 'Cobalt' theme ships with RStudio; its stylesheet href is
// "theme/default/cobalt.rstheme". 'Textmate (default)' is the out-of-the-box
// global theme and its href contains "textmate". Pinning the global theme to
// Textmate makes the "reverted to the global theme" assertion meaningful.
const PROJECT_THEME = 'Cobalt';
const GLOBAL_THEME = 'Textmate (default)';

// ID on the <link> AceThemes.java injects to apply the active editor theme CSS.
const ACE_THEME_LINK = '#rstudio-acethemes-linkelement';
const THEME_POLL = { timeout: 15000, intervals: [200, 500, 1000] };

// -- Helpers ------------------------------------------------------------------

/** Read the href of the active Ace theme stylesheet link from the DOM. */
async function getActiveThemeHref(page: Page): Promise<string> {
  return page.evaluate(
    (id) => document.querySelector(id)?.getAttribute('href') ?? '',
    ACE_THEME_LINK,
  );
}

/** Open the project Appearance tab, select a theme, then save and wait for close. */
async function setProjectEditorTheme(page: Page, themeLabel: string): Promise<void> {
  await executeCommand(page, 'projectOptions');
  const dialog = page.locator(PROJECT_OPTIONS_DIALOG);
  await expect(dialog).toBeVisible({ timeout: 15000 });
  await dialog.locator(APPEARANCE_TAB).click();

  const select = dialog.locator(PROJECT_THEME_SELECT);
  await expect(select).toBeVisible({ timeout: 5000 });
  // Wait for the async theme list to land before selecting.
  await expect(select.locator('option', { hasText: themeLabel })).toHaveCount(1, {
    timeout: 15000,
  });

  await select.selectOption({ label: themeLabel });
  // onApply reads the DOM <select>; fire change explicitly so the pane observes it.
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

/** Cancel out of a still-open dialog without persisting further changes. */
async function cancelDialog(page: Page, dialogSelector: string): Promise<void> {
  const dialog = page.locator(dialogSelector);
  await dialog.locator(DIALOG_CANCEL).click();
  await expect(dialog).not.toBeVisible({ timeout: 10000 });
}

// -- Test suite ---------------------------------------------------------------

test.describe.serial('Ignore project appearance settings', () => {
  const sandbox = useSuiteSandbox();

  // Original global editor_theme, restored in afterAll.
  let originalTheme: string | null = null;

  test.beforeAll(async ({ rstudioPage: page }) => {
    originalTheme = (await getPref(page, 'editor_theme')) as string | null;

    // Pin the global theme so the override (Cobalt) and the revert target
    // (Textmate) are distinct. setPref alone doesn't re-theme mid-session, but
    // opening the project below rebuilds the session and applies it.
    await setPref(page, 'editor_theme', GLOBAL_THEME);

    // The override indicator (and the project theme) only exist inside a project.
    await createAndOpenProject(page, sandbox.dir, 'IgnoreAppearance');
    await setProjectEditorTheme(page, PROJECT_THEME);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // Reset the new pref so it can't leak into other suites in this worker.
    try {
      await clearPref(page, 'ignore_project_appearance');
    } catch (err) {
      console.warn('[ignore_project_appearance] afterAll clearPref failed:', err);
    }

    try {
      if ((await numModalsShowing(page)) > 0) {
        await dismissAllModals(page);
      }
    } catch (err) {
      console.warn('[ignore_project_appearance] afterAll dismissAllModals failed:', err);
    }

    // Restore the original global editor_theme.
    try {
      if (originalTheme !== null) {
        await setPref(page, 'editor_theme', originalTheme);
      } else {
        await clearPref(page, 'editor_theme');
      }
    } catch (err) {
      console.warn('[ignore_project_appearance] afterAll theme restore failed:', err);
    }

    // Closing the project rebuilds the session, dropping the project pref layer
    // and re-applying the restored global theme via syncThemePrefs.
    try {
      await closeProjectIfOpen(page);
    } catch (err) {
      console.warn('[ignore_project_appearance] afterAll closeProjectIfOpen failed:', err);
    }
  });

  test('reverts to the global theme on Apply, and restores the override when cleared', async ({
    rstudioPage: page,
  }) => {
    const dialog = await openGlobalAppearance(page);

    // Precondition: the project override is active -- indicator shown, selector
    // hidden, and the live editor theme is the project's Cobalt.
    const overridePanel = dialog.locator(GLOBAL_THEME_OVERRIDE);
    await expect(overridePanel).toBeVisible({ timeout: 15000 });
    await expect(dialog.locator(GLOBAL_THEME_SELECT)).not.toBeVisible();
    await expect.poll(() => getActiveThemeHref(page), THEME_POLL).toMatch(/cobalt/i);

    const ignoreCheckbox = dialog.getByRole('checkbox', { name: IGNORE_CHECKBOX_LABEL });
    await expect(ignoreCheckbox).toBeVisible();

    // Checking the box alone must not change the pane or the live theme; the
    // change takes effect only on Apply/OK.
    await ignoreCheckbox.check();
    await expect(overridePanel).toBeVisible();
    expect(await getActiveThemeHref(page)).toMatch(/cobalt/i);

    // Apply: the pane updates immediately -- the override indicator gives way to
    // the theme selector -- and the live theme reverts to the global Textmate.
    await dialog.locator(PREFERENCES_APPLY).click();
    await expect(overridePanel).not.toBeVisible({ timeout: 10000 });
    await expect(dialog.locator(GLOBAL_THEME_SELECT)).toBeVisible({ timeout: 10000 });
    await expect.poll(() => getActiveThemeHref(page), THEME_POLL).toMatch(/textmate/i);

    // Clearing the option (uncheck + Apply) restores the project override live.
    await ignoreCheckbox.uncheck();
    await dialog.locator(PREFERENCES_APPLY).click();
    await expect(overridePanel).toBeVisible({ timeout: 10000 });
    await expect(dialog.locator(GLOBAL_THEME_SELECT)).not.toBeVisible({ timeout: 10000 });
    await expect.poll(() => getActiveThemeHref(page), THEME_POLL).toMatch(/cobalt/i);

    // A stray "Install Posit Assistant" modal (if the assistant pane regresses)
    // would sit on top of the global dialog and intercept the cancel click.
    if ((await numModalsShowing(page)) > 1) {
      await dismissAllModals(page);
      await expect(dialog).not.toBeVisible({ timeout: 10000 });
      return;
    }

    await cancelDialog(page, GLOBAL_OPTIONS_DIALOG);
  });
});
