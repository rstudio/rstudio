// Regression test for https://github.com/rstudio/rstudio/issues/17790
//
// An open GWT modal dialog blocks the Electron close path: the renderer's
// quit confirmation prompts queue behind the existing modal, leaving the
// window in a half-shut-down state until the user manually dismisses it.
// The automation bridge exposes window.rstudio.dialogs.dismissAll() so test
// teardown can clear the modal stack before the harness closes the app.
//
// This test exercises the bridge directly (open a modal, dismiss it via the
// bridge, verify the stack is empty and the IDE remains responsive).
// fixtures/desktop.fixture.ts also calls dismissAllModals() in
// shutdownRStudio(), so every Desktop test run exercises the cleanup path.

import { test, expect } from '@fixtures/rstudio.fixture';
import { CONSOLE_INPUT } from '@pages/console_pane.page';
import {
  dismissAllModals,
  executeCommand,
  numModalsShowing,
} from '@utils/commands';

const OPTIONS_OK = '#rstudio_preferences_confirm';
const DIALOG_BOX = '.gwt-DialogBox';

test.describe('Modal dismiss bridge (#17790)', { tag: ['@parallel_safe'] }, () => {
  test('dismissAll clears an open Global Options dialog', async ({ rstudioPage: page }) => {
    // Sanity check: no modals at start.
    expect(await numModalsShowing(page)).toBe(0);

    // Open Tools > Global Options. The OK button ID is the most stable
    // post-condition that the dialog has actually mounted (the dialog box
    // class alone matches before the inner content is rendered).
    await executeCommand(page, 'showOptions');
    await page.waitForSelector(OPTIONS_OK, { timeout: 15000 });
    await expect.poll(() => numModalsShowing(page), { timeout: 5000 }).toBe(1);

    // Dismiss via the automation bridge.
    await dismissAllModals(page);

    // Modal stack and DOM should both report empty.
    await expect.poll(() => numModalsShowing(page), { timeout: 5000 }).toBe(0);
    await expect(page.locator(DIALOG_BOX)).toHaveCount(0);

    // Console should still be responsive after the bridge-driven dismissal.
    await expect(page.locator(CONSOLE_INPUT)).toBeVisible();
  });

  test('dismissAll is a no-op when no modals are showing', async ({ rstudioPage: page }) => {
    expect(await numModalsShowing(page)).toBe(0);
    await dismissAllModals(page);
    expect(await numModalsShowing(page)).toBe(0);
    await expect(page.locator(CONSOLE_INPUT)).toBeVisible();
  });
});
