// The smooth_scrolling pref drives Ace's animatedScroll option (#14696).
//
// TextEditingTargetPrefsHelper binds the pref to every source editor, so a
// change applies live to open documents and to documents opened afterwards.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, executeCommand, setPref, waitForSourcePaneReset } from '@utils/commands';

async function activeEditorAnimatesScroll(page: Page): Promise<boolean | null> {
  return page.evaluate(() => {
    const editor = window.rstudio?.documents.activeEditor();
    if (!editor) return null;
    return editor.getOption('animatedScroll');
  });
}

test.describe.serial('Smooth scrolling pref', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
    await waitForSourcePaneReset(page);
    await clearPref(page, 'smooth_scrolling');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'smooth_scrolling');
    await consoleActions.resetSourcePane();
  });

  test('is off by default and applies live to the open editor', async ({ rstudioPage: page }) => {
    await expect.poll(() => activeEditorAnimatesScroll(page)).toBe(false);

    await setPref(page, 'smooth_scrolling', true);
    await expect.poll(() => activeEditorAnimatesScroll(page), { timeout: TIMEOUTS.fileOpen }).toBe(true);

    await setPref(page, 'smooth_scrolling', false);
    await expect.poll(() => activeEditorAnimatesScroll(page), { timeout: TIMEOUTS.fileOpen }).toBe(false);
  });

  test('applies to documents opened after it is enabled', async ({ rstudioPage: page }) => {
    await setPref(page, 'smooth_scrolling', true);
    try {
      const originalDocId = await page.evaluate(() => window.rstudio?.documents.active()?.id ?? null);
      expect(originalDocId).not.toBeNull();

      await executeCommand(page, 'newSourceDoc');
      await expect.poll(
        () => page.evaluate((previousId) => {
          const activeDoc = window.rstudio?.documents.active();
          return activeDoc != null && activeDoc.id !== previousId;
        }, originalDocId),
        { timeout: TIMEOUTS.fileOpen }
      ).toBe(true);
      await expect.poll(() => activeEditorAnimatesScroll(page), { timeout: TIMEOUTS.fileOpen }).toBe(true);
    } finally {
      await clearPref(page, 'smooth_scrolling');
    }
  });
});
