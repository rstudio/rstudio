// Toolbar menu buttons: where focus goes when the menu closes (#19634).
//
// Escape used to leave focus on <body>. It now returns to the menu button as the
// menu closes, before a chosen item's command runs, so the command can still move
// focus on, and a dialog it opens returns focus to the button. A click into
// another input keeps focus there.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from '@playwright/test';

import { CONSOLE_INPUT } from '@pages/console_pane.page';
import { CANCEL_BTN } from '@pages/modals.page';
import { expectHoldsFor } from '@utils/expect-holds';

const NEW_FILE_BUTTON = '#rstudio_new_file_menubutton';
const PANE_LAYOUT_BUTTON = '#rstudio_panelayout_menubutton';

// Outlasts the close handler's deferred focus restore several times over.
const RESTORE_WINDOW_MS = 500;

async function openMenuFromKeyboard(page: Page, button: string): Promise<void> {
  await page.locator(button).focus();
  await page.keyboard.press('Enter');
  await expect(page.locator(button)).toHaveAttribute('aria-expanded', 'true');
}

test.describe('Toolbar menu button focus (#19634)', () => {
  test.afterEach(async ({ rstudioPage: page }) => {
    await page.keyboard.press('Escape');
  });

  for (const [name, button] of [
    ['New File', NEW_FILE_BUTTON],
    ['Workspace Panes', PANE_LAYOUT_BUTTON],
  ]) {
    test(`Escape returns focus to the ${name} button`, async ({ rstudioPage: page }) => {
      await openMenuFromKeyboard(page, button);
      await page.keyboard.press('Escape');

      await expect(page.locator(button)).not.toHaveAttribute('aria-expanded', 'true');
      await expect(page.locator(button)).toBeFocused();
    });
  }

  test('a chosen item that moves focus keeps it', async ({ rstudioPage: page }) => {
    await openMenuFromKeyboard(page, NEW_FILE_BUTTON);
    await page.getByRole('menuitem', { name: /^R Script/ }).click();

    const editorInput = page.locator('#rstudio_Source_pane .ace_text-input:focus');
    await expect(editorInput).toHaveCount(1);
    await expectHoldsFor(RESTORE_WINDOW_MS, async () => {
      await expect(page.locator(NEW_FILE_BUTTON), 'the button must not take focus back')
        .not.toBeFocused();
      await expect(editorInput).toHaveCount(1);
    });
  });

  test('closing a dialog opened from a chosen item returns focus to the button', async ({
    rstudioPage: page,
  }) => {
    await openMenuFromKeyboard(page, NEW_FILE_BUTTON);
    await page.getByRole('menuitem', { name: /^R Documentation/ }).click();
    await page.locator(CANCEL_BTN).click();

    await expect(page.locator(CANCEL_BTN)).toHaveCount(0);
    await expect(page.locator(NEW_FILE_BUTTON)).toBeFocused();
  });

  test('clicking another input while the menu is open keeps focus there', async ({
    rstudioPage: page,
  }) => {
    // Not New File: in a small Desktop window its tall menu can cover the Console
    // input, and the forced click would land on a menu item instead.
    await openMenuFromKeyboard(page, PANE_LAYOUT_BUTTON);
    await page.locator(CONSOLE_INPUT).click({ force: true });
    await expect(page.locator(PANE_LAYOUT_BUTTON)).not.toHaveAttribute('aria-expanded', 'true');

    await expectHoldsFor(RESTORE_WINDOW_MS, async () => {
      await expect(page.locator(CONSOLE_INPUT)).toBeFocused();
    });
  });
});
