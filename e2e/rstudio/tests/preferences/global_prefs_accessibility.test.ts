import type { Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { dismissAllModals } from '@utils/commands';
import {
  DIALOG_BOX,
  GENERAL_TAB,
  GENERAL_PANEL,
  CODE_TAB,
  CODE_PANEL,
  OPTIONS_APPLY,
  openGlobalOptions,
  closeGlobalOptions,
} from '@pages/global_options.page';

// Keyboard accessibility of the Global Options dialog: initial focus, arrow-key
// navigation of the section list, and Tab/Shift+Tab off either end. The dialog
// is a GWT widget that behaves identically on Desktop and Server, and the keys
// used (Arrow / Tab / Shift+Tab) carry no platform variation -- so cross-mode
// and cross-platform, no @desktop_only. No useSuiteSandbox() either: the spec
// drives the dialog by keyboard only and never triggers Apply, so nothing
// persists to disk.
//
// The section list is an ARIA tablist (SectionChooser): only the selected tab
// is tabbable, and Up/Down moves and activates sections.
//
// Like a native modal <dialog>, Tab off either end is left to the browser, and
// nothing behind the dialog can take focus. Headless browsers cycle back into the
// page rather than reaching browser UI, so the tests check that the keydown isn't
// prevented and that focus stays in the dialog or leaves the page.

const STRAY_POPUP = 'pw_stray_popup';

// Press `key` on `selector`, then report whether the keydown was seen, whether
// anything prevented its default action, and where focus went: 'dialog', 'left
// page' (focus on <body>), or the element that took it. The event is read after
// dispatch, so listener order doesn't matter.
async function pressAndInspect(page: Page, selector: string, key: string) {
  await page.evaluate(() => {
    const w = window as unknown as Record<string, any>;
    w.__pwTabEvent = undefined;
    w.__pwTabListener = (e: KeyboardEvent) => {
      if (e.key === 'Tab') w.__pwTabEvent = e;
    };
    window.addEventListener('keydown', w.__pwTabListener, true);
  });
  try {
    await page.locator(selector).press(key);
  } finally {
    await page.evaluate(() => {
      const w = window as unknown as Record<string, any>;
      window.removeEventListener('keydown', w.__pwTabListener, true);
    });
  }
  return await page.evaluate(() => {
    const event = (window as unknown as Record<string, any>).__pwTabEvent as KeyboardEvent | undefined;
    const el = document.activeElement;
    let focus: string;
    if (el === null || el === document.body)
      focus = 'left page';
    else if (el.closest('.gwt-DialogBox') !== null)
      focus = 'dialog';
    else
      focus = `${el.tagName}#${el.id}`;
    return {
      seen: event !== undefined,
      prevented: event?.defaultPrevented ?? false,
      focus,
    };
  });
}

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

  test('Tab from the last control is left to the browser', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);

    const result = await pressAndInspect(page, OPTIONS_APPLY, 'Tab');
    expect(result).toMatchObject({ seen: true, prevented: false });
    expect(['dialog', 'left page']).toContain(result.focus);

    await closeGlobalOptions(page);
  });

  test('Shift+Tab from the first control is left to the browser', async ({ rstudioPage: page }) => {
    await openGlobalOptions(page);

    const result = await pressAndInspect(page, GENERAL_TAB, 'Shift+Tab');
    expect(result).toMatchObject({ seen: true, prevented: false });
    expect(['dialog', 'left page']).toContain(result.focus);

    await closeGlobalOptions(page);
  });

  test('a popup shown before the dialog opens is out of reach', async ({ rstudioPage: page }) => {
    // Stands in for a non-modal popup (e.g. the manipulate() controls), which
    // GWT attaches to <body>, outside the IDE container, ahead of the dialog.
    await page.evaluate((id) => {
      const button = document.createElement('button');
      button.id = id;
      button.textContent = 'stray popup';
      document.body.appendChild(button);
    }, STRAY_POPUP);
    try {
      await openGlobalOptions(page);

      const result = await pressAndInspect(page, GENERAL_TAB, 'Shift+Tab');
      expect(['dialog', 'left page']).toContain(result.focus);

      await closeGlobalOptions(page);
      await expect(page.locator(`#${STRAY_POPUP}`)).not.toHaveAttribute('inert');
    } finally {
      await page.evaluate((id) => document.getElementById(id)?.remove(), STRAY_POPUP);
    }
  });
});
