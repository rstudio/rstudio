// Where focus goes when a toolbar menu closes (#19634), for both kinds of
// toolbar menu button: ToolbarMenuButton (New File, Workspace Panes) and
// Toolbar.ToolbarPopupButton (the Help pane's topic title). Escape or a chosen
// item returns focus to the button; a command or click that moves focus keeps it.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from '@playwright/test';

import { CANCEL_BTN } from '@pages/modals.page';
import { expectHoldsFor } from '@utils/expect-holds';

const NEW_FILE_BUTTON = '#rstudio_new_file_menubutton';
const PANE_LAYOUT_BUTTON = '#rstudio_panelayout_menubutton';
const HELP_TAB = '#rstudio_workbench_tab_help';
const HELP_TITLE_BUTTON = '#rstudio_workbench_panel_help [aria-haspopup="true"]';
// A plain text box clear of both menus, so a click on it always lands and takes focus.
const GO_TO_FILE_INPUT = '#rstudio_code_search_widget input';

// Outlasts the deferred work after a menu closes several times over.
const RESTORE_WINDOW_MS = 500;

async function openMenuFromKeyboard(page: Page, button: string): Promise<void> {
  await page.locator(button).focus();
  await page.keyboard.press('Enter');
  await expect(page.locator(button)).toHaveAttribute('aria-expanded', 'true');
}

// Escape, then check focus came back to the button already marked not expanded,
// so a screen reader doesn't announce it as expanded.
async function escapeReturnsFocusCollapsed(page: Page, button: string): Promise<void> {
  const target = page.locator(button);
  // The page outlives each test, so clear a marker an earlier test left.
  await target.evaluate((el) => {
    delete el.dataset.pwExpandedOnFocus;
    el.addEventListener('focus', () => {
      el.dataset.pwExpandedOnFocus = String(el.getAttribute('aria-expanded'));
    }, { once: true });
  });
  await page.keyboard.press('Escape');

  await expect(target).toBeFocused();
  await expect(target).toHaveAttribute('data-pw-expanded-on-focus', /.*/);
  expect(await target.getAttribute('data-pw-expanded-on-focus')).not.toBe('true');
  await expect(target).not.toHaveAttribute('aria-expanded', 'true');
}

// Click the Go to File box while the button's menu is open: the click takes
// focus, and the button must not take it back. Whether the box keeps focus
// afterwards isn't checked, since Posit Assistant can take focus on its own.
async function clickAwayLeavesButtonUnfocused(page: Page, button: string): Promise<void> {
  const input = page.locator(GO_TO_FILE_INPUT);
  await input.evaluate((el) => {
    delete el.dataset.pwFocused;
    el.addEventListener('focus', () => {
      el.dataset.pwFocused = 'true';
    }, { once: true });
  });
  await input.click();

  await expect(input, 'the click must land on the Go to File box')
    .toHaveAttribute('data-pw-focused', 'true');
  await expect(page.locator(button)).not.toHaveAttribute('aria-expanded', 'true');
  await expectHoldsFor(RESTORE_WINDOW_MS, async () => {
    await expect(page.locator(button), 'the button must not take focus back')
      .not.toBeFocused();
  });
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
      await escapeReturnsFocusCollapsed(page, button);
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

  test('choosing an item with Enter keeps the menu closed', async ({ rstudioPage: page }) => {
    await openMenuFromKeyboard(page, NEW_FILE_BUTTON);
    const selected = page.locator('.gwt-MenuBar-vertical .gwt-MenuItem-selected');
    await page.keyboard.press('Home');
    await expect(selected).toHaveText(/^R Script/);
    await page.keyboard.press('Enter');

    // Focus is back on the button while Enter is still being handled; that Enter
    // must not reach the button and reopen the menu.
    const editorInput = page.locator('#rstudio_Source_pane .ace_text-input:focus');
    await expect(editorInput).toHaveCount(1);
    await expectHoldsFor(RESTORE_WINDOW_MS, async () => {
      await expect(page.locator(NEW_FILE_BUTTON)).not.toHaveAttribute('aria-expanded', 'true');
      await expect(selected).toHaveCount(0);
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

  test('clicking elsewhere while the menu is open leaves the button unfocused', async ({
    rstudioPage: page,
  }) => {
    await openMenuFromKeyboard(page, PANE_LAYOUT_BUTTON);
    await clickAwayLeavesButtonUnfocused(page, PANE_LAYOUT_BUTTON);
  });

  test.describe('Help pane topic title', () => {
    test.beforeEach(async ({ rstudioPage: page }) => {
      await page.locator(HELP_TAB).click();
      await expect(page.locator(HELP_TITLE_BUTTON)).toBeVisible();
    });

    test('Escape returns focus to the title button', async ({ rstudioPage: page }) => {
      await openMenuFromKeyboard(page, HELP_TITLE_BUTTON);
      await escapeReturnsFocusCollapsed(page, HELP_TITLE_BUTTON);
    });

    test('clicking elsewhere while the menu is open leaves the button unfocused', async ({
      rstudioPage: page,
    }) => {
      await openMenuFromKeyboard(page, HELP_TITLE_BUTTON);
      await clickAwayLeavesButtonUnfocused(page, HELP_TITLE_BUTTON);
    });
  });
});
