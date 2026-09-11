// The smooth_scrolling pref drives Ace's animatedScroll option (#14696).
//
// TextEditingTargetPrefsHelper binds the pref to every source editor, so a
// change applies live to open documents and to documents opened afterwards.

import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { TIMEOUTS } from '@utils/constants';
import { clearPref, executeCommand, setPref, waitForSourcePaneReset } from '@utils/commands';

declare global {
  interface Window {
    smoothScrollProbe?: { samples: number[]; dispose: () => void };
  }
}

async function startScrollProbe(page: Page): Promise<void> {
  await page.evaluate(() => {
    const editor = window.rstudio?.documents.activeEditor();
    if (!editor) throw new Error('No active source editor');
    window.smoothScrollProbe?.dispose();
    const samples: number[] = [];
    const listener = (scrollTop: number) => {
      // Observe real scroll positions while Ace interpolates, rather than
      // merely checking the preference or whether animateScrolling was called.
      if (editor.renderer.$scrollAnimation) samples.push(scrollTop);
    };
    editor.session.on('changeScrollTop', listener);
    window.smoothScrollProbe = {
      samples,
      dispose: () => editor.session.off('changeScrollTop', listener),
    };
  });
}

async function expectScrollAnimation(page: Page, enabled: boolean): Promise<void> {
  if (enabled) {
    await expect.poll(() => page.evaluate(() =>
      new Set(window.smoothScrollProbe?.samples).size
    )).toBeGreaterThan(2);
  }
  await expect.poll(() => page.evaluate(() =>
    window.rstudio?.documents.activeEditor()?.renderer.$scrollAnimation == null
  )).toBe(true);
  if (!enabled) {
    expect(await page.evaluate(() => window.smoothScrollProbe?.samples)).toEqual([]);
  }
}

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

  test.afterEach(async ({ rstudioPage: page }) => {
    await page.evaluate(() => {
      window.smoothScrollProbe?.dispose();
      delete window.smoothScrollProbe;
    });
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

  for (const enabled of [true, false]) {
    test(`Go to Line ${enabled ? 'animates' : 'jumps'} when smooth scrolling is ${enabled}`, async ({ rstudioPage: page }) => {
      await setPref(page, 'smooth_scrolling', enabled);
      await page.evaluate(() => {
        const editor = window.rstudio!.documents.activeEditor()!;
        editor.setValue(Array.from({ length: 300 }, (_, i) => `# line ${i + 1}`).join('\n'), -1);
        editor.gotoLine(1, 0, false);
      });
      await startScrollProbe(page);
      // Exercise RStudio's Go to Line dialog, which uses a different path
      // from Ace's gotoLine() API.
      await consoleActions.goToLine(200);
      await expect.poll(() => page.evaluate(() =>
        window.rstudio?.documents.activeEditor()?.getCursorPosition().row
      )).toBe(199);
      await expectScrollAnimation(page, enabled);
      await expect.poll(() => page.evaluate(() =>
        window.rstudio?.documents.activeEditor()?.getFirstVisibleRow()
      )).toBeGreaterThan(0);
    });

    test(`arrow navigation ${enabled ? 'animates' : 'jumps'} when smooth scrolling is ${enabled}`, async ({ rstudioPage: page }) => {
      await setPref(page, 'smooth_scrolling', enabled);
      await page.evaluate(() => {
        const editor = window.rstudio!.documents.activeEditor()!;
        editor.setValue(Array.from({ length: 300 }, (_, i) => `# line ${i + 1}`).join('\n'), -1);
        editor.gotoLine(150, 0, false);
      });
      // Wait for layout before choosing a row at the viewport boundary.
      await expect.poll(() => page.evaluate(() =>
        window.rstudio?.documents.activeEditor()?.getFirstVisibleRow()
      )).toBeGreaterThan(0);

      for (const key of ['ArrowDown', 'ArrowUp', 'Shift+ArrowDown', 'Shift+ArrowUp']) {
        const before = await page.evaluate((key) => {
          const editor = window.rstudio!.documents.activeEditor()!;
          const row = key.endsWith('ArrowDown') ? editor.getLastVisibleRow() : editor.getFirstVisibleRow();
          editor.selection.setRange({ start: { row, column: 0 }, end: { row, column: 0 } });
          editor.focus();
          return { row, scrollTop: editor.renderer.scrollTop };
        }, key);
        await startScrollProbe(page);
        await page.keyboard.press(key);
        await expect.poll(() => page.evaluate(() =>
          window.rstudio?.documents.activeEditor()?.getCursorPosition().row
        )).toBe(before.row + (key.endsWith('ArrowDown') ? 1 : -1));
        await expectScrollAnimation(page, enabled);
        expect(await page.evaluate(() =>
          window.rstudio!.documents.activeEditor()!.renderer.scrollTop
        )).not.toBe(before.scrollTop);
      }
    });
  }
});
