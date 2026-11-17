// Toolbar keyboard navigation (#5960), checked on the main toolbar. Arrow keys,
// Home and End move between items without wrapping, and Tab stops once per
// group of items, on the one focused last. The Go to File box keeps the arrow
// keys for editing, so it has its own Tab stop and splits the toolbar in two.

import { test, expect } from '@fixtures/rstudio.fixture';
import type { Locator, Page } from '@playwright/test';

import { documentCloseAllNoSave, executeCommand } from '@utils/commands';

const NEW_FILE_BUTTON = '#rstudio_new_file_menubutton';
const PANE_LAYOUT_BUTTON = '#rstudio_panelayout_menubutton';
const GO_TO_FILE_INPUT = '#rstudio_code_search_widget input';

function mainToolbar(page: Page): Locator {
  return page.getByRole('toolbar', { name: 'Main', exact: true });
}

// The focused element's id, or a marker for focus outside the main toolbar.
async function focusedItem(page: Page): Promise<string> {
  return page.evaluate(() => {
    const el = document.activeElement as HTMLElement | null;
    const toolbar = document.querySelector('[role="toolbar"][aria-label="Main"]');
    if (!el || !toolbar || !toolbar.contains(el))
      return '<outside main toolbar>';
    if (el.id === '')
      el.id = `pw-toolbar-item-${Math.random().toString(36).slice(2)}`;
    return el.id;
  });
}

// Whether the focused element follows the Go to File box in document order.
async function focusFollowsGoToFile(page: Page): Promise<boolean> {
  return page.evaluate((selector) => {
    const input = document.querySelector(selector);
    const el = document.activeElement;
    return !!input && !!el &&
      (input.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0;
  }, GO_TO_FILE_INPUT);
}

test.describe('Toolbar keyboard navigation (#5960)', () => {
  test.beforeEach(async ({ rstudioPage: page }) => {
    await expect(mainToolbar(page)).toBeVisible();
  });

  test('Tab stops once before, at and after the Go to File box', async ({
    rstudioPage: page,
  }) => {
    await page.locator(NEW_FILE_BUTTON).focus();
    await page.keyboard.press('ArrowRight');
    const arrowedTo = await focusedItem(page);
    expect(arrowedTo).not.toBe('<outside main toolbar>');
    expect(arrowedTo).not.toBe('rstudio_new_file_menubutton');

    await page.keyboard.press('Tab');
    await expect(page.locator(GO_TO_FILE_INPUT)).toBeFocused();

    await page.keyboard.press('Tab');
    const afterBox = await focusedItem(page);
    expect(afterBox).not.toBe('<outside main toolbar>');
    expect(await focusFollowsGoToFile(page), 'a button after the box').toBe(true);

    await page.keyboard.press('Tab');
    expect(await focusedItem(page), 'Tab leaves the toolbar').toBe('<outside main toolbar>');

    // Coming back lands on the item focused last in each group.
    await page.keyboard.press('Shift+Tab');
    expect(await focusedItem(page)).toBe(afterBox);
    await page.keyboard.press('Shift+Tab');
    await expect(page.locator(GO_TO_FILE_INPUT)).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    expect(await focusedItem(page)).toBe(arrowedTo);
  });

  test('arrows move into the Go to File box but not out of it', async ({
    rstudioPage: page,
  }) => {
    const input = page.locator(GO_TO_FILE_INPUT);
    await page.locator(NEW_FILE_BUTTON).focus();
    for (let i = 0; i < 20; i++) {
      if (await input.evaluate((el) => el === document.activeElement))
        break;
      await page.keyboard.press('ArrowRight');
    }
    await expect(input, 'Right arrow reaches the box').toBeFocused();

    await input.fill('abc');
    for (const key of ['ArrowLeft', 'ArrowRight', 'Home', 'End'])
      await page.keyboard.press(key);
    await expect(input).toBeFocused();
    await input.fill('');
    await page.keyboard.press('Escape');

    // Tab lands on the group's remembered item, which needn't be next to the box.
    await input.focus();
    await page.keyboard.press('Tab');
    expect(await focusFollowsGoToFile(page)).toBe(true);
    for (let i = 0; i < 20; i++) {
      if (await input.evaluate((el) => el === document.activeElement))
        break;
      await page.keyboard.press('ArrowLeft');
    }
    await expect(input, 'Left arrow reaches the box from after it').toBeFocused();
  });

  test('Home and End reach the ends without wrapping', async ({ rstudioPage: page }) => {
    await page.locator(PANE_LAYOUT_BUTTON).focus();
    await page.keyboard.press('Home');
    await expect(page.locator(NEW_FILE_BUTTON)).toBeFocused();
    await page.keyboard.press('ArrowLeft');
    await expect(page.locator(NEW_FILE_BUTTON)).toBeFocused();

    await page.keyboard.press('End');
    const last = await focusedItem(page);
    expect(last).not.toBe('<outside main toolbar>');
    await page.keyboard.press('ArrowRight');
    expect(await focusedItem(page)).toBe(last);
  });

  test('a button a menu hands focus back to becomes the Tab stop', async ({
    rstudioPage: page,
  }) => {
    const paneLayout = page.locator(PANE_LAYOUT_BUTTON);
    await paneLayout.focus();
    await page.keyboard.press('Enter');
    await expect(paneLayout).toHaveAttribute('aria-expanded', 'true');
    await page.keyboard.press('Escape');
    await expect(paneLayout).toBeFocused();

    await page.locator(GO_TO_FILE_INPUT).focus();
    await page.keyboard.press('Tab');
    await expect(paneLayout).toBeFocused();
  });

  test('Focus Main Toolbar returns to the button focused last', async ({
    rstudioPage: page,
  }) => {
    await page.locator(NEW_FILE_BUTTON).focus();
    await page.keyboard.press('ArrowRight');
    const arrowedTo = await focusedItem(page);
    await page.locator(GO_TO_FILE_INPUT).focus();

    await executeCommand(page, 'focusMainToolbar');
    await expect.poll(() => focusedItem(page)).toBe(arrowedTo);
  });

  test('the Tab stop moves off a button that becomes disabled', async ({
    rstudioPage: page,
  }) => {
    const save = mainToolbar(page).locator('[id^="rstudio_tb_savesourcedoc"]');
    await executeCommand(page, 'newSourceDoc');
    await page.locator('#rstudio_Source_pane .ace_text-input').last().click({ force: true });
    await page.keyboard.type('x');
    await expect(save).toBeEnabled();
    await save.focus();

    try {
      // With the only document gone, Save is disabled; a disabled Tab stop
      // would take the whole group out of the Tab order.
      await documentCloseAllNoSave(page);
      await expect(save).toBeDisabled();
      await page.locator(GO_TO_FILE_INPUT).focus();
      await page.keyboard.press('Shift+Tab');
      await expect(page.locator(NEW_FILE_BUTTON)).toBeFocused();
    } finally {
      await documentCloseAllNoSave(page);
    }
  });

  test('buttons added later join the toolbar, and leave it tabbable', async ({
    rstudioPage: page,
  }) => {
    // Stands in for a widget a toolbar adds after it is shown, then hands to
    // another container.
    const tabIndexes = await page.evaluate(async () => {
      const toolbar = document.querySelector('[role="toolbar"][aria-label="Main"]')!;
      const button = document.createElement('button');
      button.id = 'pw-added-button';
      toolbar.querySelector('#rstudio_new_file_menubutton')!.parentElement!.appendChild(button);
      const settle = () => new Promise((resolve) => setTimeout(resolve, 50));
      await settle();
      const inToolbar = button.tabIndex;
      document.body.appendChild(button);
      await settle();
      const moved = button.tabIndex;
      button.remove();
      return { inToolbar, moved };
    });
    expect(tabIndexes).toEqual({ inToolbar: -1, moved: 0 });
  });
});
