import type { Locator, Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { dismissAllModals, executeCommand, numModalsShowing } from '@utils/commands';
import { closeProjectIfOpen, createAndOpenProject } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import {
  ACCESSIBILITY_PANEL,
  APPEARANCE_TAB,
  APPEARANCE_PREVIEW,
  CODE_TAB,
  CODE_EDITING_PANEL,
  CODE_COMPLETION_TAB,
  CODE_COMPLETION_PANEL,
  GENERAL_TAB,
  GENERAL_PANEL,
  OPTIONS_OK,
  OPTIONS_CANCEL,
  OPTIONS_APPLY,
  PANE_LAYOUT_TAB,
  PANE_LAYOUT_PANEL,
  SECTION_TABS,
  SPELLING_TAB,
  SPELLING_PANEL,
  closeGlobalOptions,
  openGlobalOptions,
} from '@pages/global_options.page';

// Tall enough that the window doesn't cap the dialog's height.
const FULL_SIZE_VIEWPORT = { width: 800, height: 900 };

// Run fn with the window at the given size and a preferences dialog open, then
// close the dialog and restore the window size. When fn fails, the dialog is
// dismissed through the automation bridge rather than its Cancel button, so a
// failing cleanup can't replace fn's error.
async function withOptionsDialog(
  page: Page,
  size: { width: number; height: number },
  fn: () => Promise<void>,
  open: () => Promise<void> = () => openGlobalOptions(page),
): Promise<void> {
  const originalSize = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
  await page.setViewportSize(size);
  try {
    await open();
    await fn();
    await closeGlobalOptions(page);
  } catch (err) {
    await dismissAllModals(page).catch(() => {});
    throw err;
  } finally {
    await page.setViewportSize(originalSize);
  }
}

// Visit each of the pane's tabs (panes with one tab hide its header) and check
// that neither the pane container nor the tab's scrolling layer overflows.
async function expectPaneTabsFit(pane: Locator, name: string): Promise<void> {
  const tabs = pane.locator('[role="tab"]:visible');
  const tabCount = await tabs.count();
  for (let i = 0; i < Math.max(tabCount, 1); i++) {
    let label = name;
    if (tabCount > 1) {
      await tabs.nth(i).click();
      label += ` > ${await tabs.nth(i).innerText()}`;
    }

    await expect.poll(() => pane.evaluate(element => {
      const scrollers = [
        element.parentElement!,
        ...element.querySelectorAll<HTMLElement>('.gwt-TabLayoutPanelContentContainer > div'),
      ].filter(scroller => scroller.offsetParent !== null);

      // Measuring nothing must not pass.
      if (scrollers.length === 0)
        return Infinity;

      return Math.max(...scrollers.map(scroller => Math.max(
        scroller.scrollHeight - scroller.clientHeight,
        scroller.scrollWidth - scroller.clientWidth)));
    }), { message: label }).toBeLessThanOrEqual(1);
  }
}

// Check every section and tab of the open Global Options dialog. Each pane is
// measured once, so a row that arrives later over RPC isn't seen; the panes
// that load rows that way have plenty of room to spare.
async function expectAllPanesFit(page: Page): Promise<void> {
  const sectionIds = await page.locator(`${SECTION_TABS}:visible`)
    .evaluateAll(elements => elements.map(element => element.id));
  expect(sectionIds).toContain(PANE_LAYOUT_TAB.slice(1));

  for (const sectionId of sectionIds) {
    await page.locator(`#${sectionId}`).click();
    const pane = page.locator(`#${sectionId}_panel`);
    await expect(pane).toBeVisible();
    await expectPaneTabsFit(pane, sectionId);
  }
}

test.describe('Global Options layout', () => {
  test('every pane fits the full-size dialog without scrolling', async ({ rstudioPage: page }) => {
    await withOptionsDialog(page, FULL_SIZE_VIEWPORT, () => expectAllPanesFit(page));
  });

  test('Accessibility Options fits without the section chooser', async ({ rstudioPage: page }) => {
    const openAccessibilityOptions = async () => {
      await executeCommand(page, 'showAccessibilityOptions');
      await page.waitForSelector(OPTIONS_OK, { timeout: 15000 });
    };

    await withOptionsDialog(page, FULL_SIZE_VIEWPORT, async () => {
      const pane = page.locator(ACCESSIBILITY_PANEL);
      await expect(pane).toBeVisible();
      await expect(page.locator(SECTION_TABS)).not.toHaveCount(0);
      await expect(page.locator(`${SECTION_TABS}:visible`)).toHaveCount(0);
      await expectPaneTabsFit(pane, 'Accessibility Options');
    }, openAccessibilityOptions);
  });

  test('panes fit the dialog while an open window is resized', async ({ rstudioPage: page }) => {
    await withOptionsDialog(page, FULL_SIZE_VIEWPORT, async () => {
      for (const height of [641, 480, 900]) {
        await page.setViewportSize({ width: 800, height });

        // Cover both a tabbed pane and a pane with its tab header hidden.
        for (const [tab, panel] of [[GENERAL_TAB, GENERAL_PANEL], [SPELLING_TAB, SPELLING_PANEL]]) {
          await page.locator(tab).click();
          await expect.poll(() => page.locator(panel).evaluate(element => {
            const container = element.parentElement!;
            return container.scrollHeight - container.clientHeight;
          })).toBeLessThanOrEqual(1);
        }

        await page.locator(APPEARANCE_TAB).click();
        await expect(page.locator(APPEARANCE_PREVIEW)).toBeVisible();
        for (const button of [OPTIONS_OK, OPTIONS_CANCEL, OPTIONS_APPLY]) {
          await expect(page.locator(button)).toBeInViewport({ ratio: 1 });
        }
      }
    });
  });

  test('Pane Layout fits a dialog capped by a short window', async ({ rstudioPage: page }) => {
    await withOptionsDialog(page, FULL_SIZE_VIEWPORT, async () => {
      await page.locator(PANE_LAYOUT_TAB).click();

      // Short enough to cap the dialog, yet tall enough that the tab lists
      // needn't drop below their minimum height.
      for (const height of [740, 641]) {
        await page.setViewportSize({ width: 800, height });
        await expectPaneTabsFit(page.locator(PANE_LAYOUT_PANEL), `Pane Layout at height ${height}`);
      }
    });
  });

  test('tall tab content scrolls without moving the tab header or footer', async ({ rstudioPage: page }) => {
    await withOptionsDialog(page, { width: 800, height: 480 }, async () => {
      await page.locator(CODE_TAB).click();
      await page.locator(CODE_COMPLETION_TAB).click();

      const lastInput = page.locator(CODE_COMPLETION_PANEL).locator('input:visible').last();
      await lastInput.scrollIntoViewIfNeeded();
      await lastInput.click();
      await expect(lastInput).toBeInViewport({ ratio: 1 });
      await expect(lastInput).toBeFocused();
      await expect(page.locator(CODE_COMPLETION_TAB)).toBeInViewport({ ratio: 1 });
      await expect(page.locator(OPTIONS_CANCEL)).toBeInViewport({ ratio: 1 });
      await expect.poll(() => lastInput.evaluate(element => {
        for (let parent = element.parentElement; parent; parent = parent.parentElement) {
          if (parent.scrollTop > 0 && /auto|scroll/.test(getComputedStyle(parent).overflowY))
            return true;
        }
        return false;
      })).toBe(true);
    });
  });
});

// A project adds rows to some panes, e.g. the pointer to the project's options
// under Code > Editing.
test.describe.serial('Global Options layout with a project open', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await createAndOpenProject(page, sandbox.dir, 'GlobalOptionsLayout');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    // A dialog left open would block the project close.
    if ((await numModalsShowing(page)) > 0)
      await dismissAllModals(page);
    await closeProjectIfOpen(page);
  });

  test('every pane fits the full-size dialog without scrolling', async ({ rstudioPage: page }) => {
    await withOptionsDialog(page, FULL_SIZE_VIEWPORT, async () => {
      await page.locator(CODE_TAB).click();
      await expect(page.locator(CODE_EDITING_PANEL)
        .getByText('Some settings may be overridden by project options.')).toBeVisible();
      await expectAllPanesFit(page);
    });
  });
});
