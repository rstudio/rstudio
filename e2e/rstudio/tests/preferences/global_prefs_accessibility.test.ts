import { test, expect } from '@fixtures/rstudio.fixture';
import { dismissAllModals } from '@utils/commands';
import {
  DIALOG_BOX,
  GENERAL_TAB,
  GENERAL_PANEL,
  CODE_TAB,
  CODE_PANEL,
  CONSOLE_TAB,
  OPTIONS_APPLY,
  openGlobalOptions,
  closeGlobalOptions,
} from '@pages/global_options.page';

// Keyboard accessibility of the Global Options dialog: initial focus, arrow-key
// navigation of the section list, and Tab/Shift+Tab focus wrapping. The dialog
// is a GWT widget that behaves identically on Desktop and Server, and the keys
// used (Arrow / Tab / Shift+Tab) carry no platform variation -- so cross-mode
// and cross-platform, no @desktop_only. No useSuiteSandbox() either: the spec
// drives the dialog by keyboard only and never triggers Apply, so nothing
// persists to disk.
//
// The section list is an ARIA tablist (SectionChooser): only the selected tab
// is tabbable, Up/Down moves and activates sections, and the dialog traps focus
// so Tab from the last control (Apply) returns to the selected section tab.
test.describe('Global Options keyboard accessibility', () => {
  // Safety net: a focus assertion that fails mid-test leaves the dialog open,
  // which would break the next test. Dismiss any lingering dialog.
  test.afterEach(async ({ rstudioPage: page }) => {
    if (await page.locator(DIALOG_BOX).count() > 0) {
      await dismissAllModals(page);
      await page.waitForSelector(DIALOG_BOX, { state: 'detached', timeout: 10000 });
    }
  });

  test('opens with keyboard focus on the first section', async ({ rstudioPage: page }) => {
    // Original: test_global_preferences_initial_keyboard_focus
    await openGlobalOptions(page);
    await expect(page.locator(GENERAL_TAB)).toBeFocused();
    await closeGlobalOptions(page);
  });

  test('Arrow Down selects and activates the next section', async ({ rstudioPage: page }) => {
    // Original: test_global_preferences_keyboard_select_next_pane
    await openGlobalOptions(page);

    // General is the default-selected section; ArrowDown moves to Code, focuses
    // it, and activates its panel (the previous panel is no longer shown).
    await page.locator(GENERAL_TAB).press('ArrowDown');
    await expect(page.locator(CODE_TAB)).toBeFocused();
    await expect(page.locator(CODE_PANEL)).toBeVisible();
    await expect(page.locator(GENERAL_PANEL)).toBeHidden();

    await closeGlobalOptions(page);
  });

  test('forward Tab from the last control wraps to the selected section', async ({ rstudioPage: page }) => {
    // Original: test_global_preferences_keyboard_forward_tab_wrap
    await openGlobalOptions(page);

    // General is selected on open; Tab from the last control (Apply) wraps the
    // dialog's focus back to the selected section tab.
    await page.locator(OPTIONS_APPLY).press('Tab');
    await expect(page.locator(GENERAL_TAB)).toBeFocused();

    await closeGlobalOptions(page);
  });

  test('backward Shift+Tab from the first control goes to the last control', async ({ rstudioPage: page }) => {
    // Original: test_global_preferences_keyboard_backward_tab_wrap
    await openGlobalOptions(page);

    // Shift+Tab from the selected section tab (first control) wraps to the last
    // control, the Apply button.
    await page.locator(GENERAL_TAB).press('Shift+Tab');
    await expect(page.locator(OPTIONS_APPLY)).toBeFocused();

    await closeGlobalOptions(page);
  });

  test('forward Tab wrap targets the currently-selected section', async ({ rstudioPage: page }) => {
    // Original: test_global_preferences_keyboard_forward_tab_wrap_changed_panel
    await openGlobalOptions(page);

    // Move to Code: Tab from Apply now wraps to Code, not back to General.
    await page.locator(GENERAL_TAB).press('ArrowDown');
    await expect(page.locator(CODE_TAB)).toBeFocused();
    await page.locator(OPTIONS_APPLY).press('Tab');
    await expect(page.locator(CODE_TAB)).toBeFocused();

    // Move to Console (the third section): the wrap target follows again.
    await page.locator(CODE_TAB).press('ArrowDown');
    await expect(page.locator(CONSOLE_TAB)).toBeFocused();
    await page.locator(OPTIONS_APPLY).press('Tab');
    await expect(page.locator(CONSOLE_TAB)).toBeFocused();

    await closeGlobalOptions(page);
  });

  test('backward Shift+Tab from a changed section goes to the last control', async ({ rstudioPage: page }) => {
    // Original: test_global_preferences_keyboard_back_tab_wrap_changed_panel
    await openGlobalOptions(page);

    // From Code, Shift+Tab reaches Apply; Tab returns to Code.
    await page.locator(GENERAL_TAB).press('ArrowDown');
    await expect(page.locator(CODE_TAB)).toBeFocused();
    await page.locator(CODE_TAB).press('Shift+Tab');
    await expect(page.locator(OPTIONS_APPLY)).toBeFocused();
    await page.locator(OPTIONS_APPLY).press('Tab');
    await expect(page.locator(CODE_TAB)).toBeFocused();

    // From Console, Shift+Tab also reaches Apply.
    await page.locator(CODE_TAB).press('ArrowDown');
    await expect(page.locator(CONSOLE_TAB)).toBeFocused();
    await page.locator(CONSOLE_TAB).press('Shift+Tab');
    await expect(page.locator(OPTIONS_APPLY)).toBeFocused();

    await closeGlobalOptions(page);
  });
});
