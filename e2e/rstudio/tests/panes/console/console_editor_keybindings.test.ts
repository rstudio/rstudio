// Custom "Editor" keyboard shortcuts apply to the Console input (#12379).
//
// Bindings changed in Tools > Modify Keyboard Shortcuts for the Editor scope
// are pushed to source editors through SetEditorCommandBindingsEvent. The
// console input is an Ace editor too, so it must honor the same rebinding.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { waitForConsoleFocus } from '@pages/console_pane.page';
import { executeCommand } from '@utils/commands';
import { YES_BTN } from '@pages/modals.page';
import {
  getConsoleCursorPosition,
  getConsoleScreenRowCount,
  setConsoleInput,
} from '@utils/console';

// Keybindings live in <config home>/keybindings/. The editor bindings are in
// editor_bindings.json, but the dialog's Reset button also rewrites the app
// and addin bindings, so all three are handled. Desktop and spawned-server
// workers run with a sandboxed RSTUDIO_CONFIG_HOME, but an external server
// shares its config with the developer, so any pre-existing files are set
// aside and restored through the R session.
const BINDINGS_FILES = [
  'file.path(Sys.getenv("RSTUDIO_CONFIG_HOME",',
  '  unset = file.path(Sys.getenv("XDG_CONFIG_HOME", unset = "~/.config"), "rstudio")),',
  '  "keybindings", c("editor_bindings.json", "rstudio_bindings.json", "addins.json"))',
].join(' ');

const BACKUP_BINDINGS = [
  `for (f in ${BINDINGS_FILES})`,
  'if (file.exists(f)) file.rename(f, paste0(f, ".e2e-backup"))',
].join(' ');

const RESTORE_BINDINGS = [
  `for (f in ${BINDINGS_FILES}) {`,
  'unlink(f);',
  'if (file.exists(paste0(f, ".e2e-backup"))) file.rename(paste0(f, ".e2e-backup"), f)',
  '}',
].join(' ');

// Unbound in every RStudio keymap and in Ace's defaults, so it can only reach
// the console through the rebinding under test.
const NEW_SHORTCUT = 'Control+Alt+Shift+K';
const NEW_SHORTCUT_LABEL = 'Ctrl+Alt+Shift+K';

async function reloadAndWait(page: Page): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, { timeout: 30000 });
}

test.describe.serial('Console honors custom editor keybindings', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.executeInConsole(BACKUP_BINDINGS);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(RESTORE_BINDINGS);

    // the rebinding also lives in the running Ace editors; a reload rebuilds
    // them from the restored (default) bindings
    await reloadAndWait(page);
  });

  test('Remove Word Left rebound in the shortcuts dialog works in the console', async ({
    rstudioPage: page,
  }) => {
    await executeCommand(page, 'modifyKeyboardShortcuts');
    const dialog = page.getByRole('dialog', { name: 'Keyboard Shortcuts', exact: true });
    await expect(dialog).toBeVisible();

    // the filter widget applies on each keystroke
    const filter = dialog.locator('#rstudio_kybrd_shrtcts_fltr input');
    await filter.pressSequentially('Remove Word Left');
    // one row per default binding (macOS has two); rebinding one row keeps
    // the other alternatives
    const row = dialog.getByRole('row', { name: /^Remove Word Left / }).first();
    await expect(row).toBeVisible();
    await expect(row).toContainText('Editor');

    // clicking the Shortcut cell opens an inline editor that records the
    // next key combination; Enter commits it
    const shortcutCell = row.getByRole('cell').nth(1);
    await shortcutCell.click();
    await expect(shortcutCell.locator('input')).toBeVisible();
    await page.keyboard.press(NEW_SHORTCUT);
    await expect(shortcutCell.locator('input')).toHaveValue(NEW_SHORTCUT_LABEL);
    await page.keyboard.press('Enter');
    await expect(shortcutCell).toContainText(NEW_SHORTCUT_LABEL);

    await dialog.getByRole('button', { name: 'Apply', exact: true }).click();
    await expect(dialog).toBeHidden();

    // the console input is an Ace editor; the rebound command must fire there
    const input = consoleActions.consolePane.consoleInput;
    await input.click({ force: true });
    await waitForConsoleFocus(page);
    await setConsoleInput(page, 'alpha beta');
    await expect.poll(() => consoleActions.consolePane.consoleInputValue()).toBe('alpha beta');

    await page.keyboard.press(NEW_SHORTCUT);
    await expect.poll(() => consoleActions.consolePane.consoleInputValue()).toBe('alpha ');

    await setConsoleInput(page, '');
  });

  test('the saved rebinding is applied to the console at startup', async ({
    rstudioPage: page,
  }) => {
    // a fresh client loads editor_bindings.json and pushes it to the editors
    // that exist at that point; the console must be among them even when no
    // source document is open
    await reloadAndWait(page);

    const input = consoleActions.consolePane.consoleInput;
    await input.click({ force: true });
    await waitForConsoleFocus(page);
    await setConsoleInput(page, 'gamma delta');
    await expect.poll(() => consoleActions.consolePane.consoleInputValue()).toBe('gamma delta');

    await page.keyboard.press(NEW_SHORTCUT);
    await expect.poll(() => consoleActions.consolePane.consoleInputValue()).toBe('gamma ');

    await setConsoleInput(page, '');
  });

  test('Reset drops the rebinding but keeps console Home/End navigation', async ({
    rstudioPage: page,
  }) => {
    await executeCommand(page, 'modifyKeyboardShortcuts');
    const dialog = page.getByRole('dialog', { name: 'Keyboard Shortcuts', exact: true });
    await expect(dialog).toBeVisible();

    await dialog.locator('#rstudio_kybrd_shrtcts_rst').click();
    const confirm = page.getByRole('alertdialog', { name: 'Reset Keyboard Shortcuts' });
    await expect(confirm).toBeVisible();
    await confirm.locator(YES_BTN).click();
    await expect(confirm).toBeHidden();

    await dialog.getByRole('button', { name: 'Cancel', exact: true }).click();
    await expect(dialog).toBeHidden();

    // the reset reached the console: the custom shortcut no longer fires there
    const input = consoleActions.consolePane.consoleInput;
    await input.click({ force: true });
    await waitForConsoleFocus(page);
    await setConsoleInput(page, 'epsilon zeta');
    await expect.poll(() => consoleActions.consolePane.consoleInputValue()).toBe('epsilon zeta');

    await input.press(NEW_SHORTCUT);
    expect(await consoleActions.consolePane.consoleInputValue()).toBe('epsilon zeta');

    // resetting reinstalls Ace's defaults, which must not undo the console's
    // whole-line Home / End behavior on a soft-wrapped command (#18447)
    const command = `x <- c(${Array.from({ length: 80 }, (_, i) => i + 1).join(', ')})`;
    await setConsoleInput(page, command);
    expect(await getConsoleScreenRowCount(page)).toBeGreaterThan(1);

    await input.press('Home');
    await expect.poll(() => getConsoleCursorPosition(page)).toEqual({ row: 0, column: 0 });

    await input.press('End');
    await expect
      .poll(() => getConsoleCursorPosition(page))
      .toEqual({ row: 0, column: command.length });

    await setConsoleInput(page, '');
  });
});
