import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { setPref, clearPref } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import type { Page } from '@playwright/test';

// Inline LaTeX / math previews in the source editor (line widgets for $$
// display math, a popup for inline math), rendered client-side by the
// MathJax 4 bundle served from the session's /mathjax4/ URI.
test.describe('Inline LaTeX math previews', () => {
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  // Move the cursor into the editor via the automation bridge; the resulting
  // cursor-changed event arms the idle monitor, which fires the LaTeX
  // preview after its 700ms idle delay. Line numbers are 1-based.
  const moveCursorTo = async (page: Page, line: number, column: number) => {
    await page.evaluate(
      ([lineNumber, columnNumber]) => {
        const editor = window.rstudio?.documents.activeEditor();
        if (!editor)
          throw new Error('no active editor');
        editor.focus();
        editor.gotoLine(lineNumber, columnNumber);
      },
      [line, column]
    );
  };

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);

    await consoleActions.resetSourcePane();
    await setPref(page, 'latexPreviewOnCursorIdle', 'always');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await clearPref(page, 'latexPreviewOnCursorIdle');
  });

  test('MathJax 4 is loaded from the local bundle', async ({ rstudioPage: page }) => {
    // the loader is invoked at session init; the bundle comes from the
    // /mathjax4/ URI handler rather than a CDN
    const script = page.locator('script[src*="mathjax4/tex-mml-chtml.js"]');
    await expect(script).toBeAttached({ timeout: 30000 });

    await expect
      .poll(async () => await page.evaluate(() => (window as any).MathJax?.version ?? ''), {
        timeout: 60000,
      })
      .toMatch(/^4\./);

    // our configuration survived the load: inline $-math is enabled, and
    // dynamically-loaded font resources resolve locally rather than via CDN
    const config = await page.evaluate(() => ({
      inlineMath: JSON.stringify((window as any).MathJax?.config?.tex?.inlineMath ?? null),
      fontPath: (window as any).MathJax?.config?.output?.fontPath ?? null,
    }));
    expect(config.inlineMath).toContain('"$"');
    expect(config.fontPath).toContain('mathjax4/fonts');
  });

  test('display math renders as a line-widget preview', async ({ rstudioPage: page }) => {
    const fileName = `math_preview_widget_${Date.now()}.Rmd`;
    const content = heredoc`
      ---
      title: "Math"
      ---

      Some prose, then math:

      $$
      e = mc^2
      $$
    `;

    await sourceActions.createAndOpenFile(fileName, content);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });

    // place the cursor inside the display math chunk (line of 'e = mc^2')
    await moveCursorTo(page, 8, 2);

    // the preview line widget renders typeset CHTML output
    const widget = page.locator('.rstudio-mathjax-root mjx-container');
    await expect(widget).toBeVisible({ timeout: 60000 });

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });

  test('inline math renders a popup preview', async ({ rstudioPage: page }) => {
    const fileName = `math_preview_popup_${Date.now()}.Rmd`;
    const content = heredoc`
      ---
      title: "Math"
      ---

      Einstein wrote $e = mc^2$ and moved on.
    `;

    await sourceActions.createAndOpenFile(fileName, content);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });

    // place the cursor inside the inline math region
    await moveCursorTo(page, 5, 20);

    // the popup preview typesets outside of any line widget
    const popupMath = page.locator('mjx-container:not(.rstudio-mathjax-root mjx-container)');
    await expect(popupMath).toBeVisible({ timeout: 60000 });

    // watch the popup's math element: rendered TeX errors must never enter
    // the visible DOM (typesets happen in a hidden scratch element, and a
    // failed render keeps the previous output). track scratch-element
    // removals on the parent as the "a re-render completed" signal.
    await page.evaluate(() => {
      const container = Array.from(document.querySelectorAll('mjx-container')).find(
        (c) => (c as HTMLElement).offsetParent != null
      );
      const el = container!.parentElement!;
      (window as any).__merrorSeen = false;
      (window as any).__renderAttempts = 0;
      new MutationObserver(() => {
        if (el.querySelector('mjx-merror, [data-mjx-error]'))
          (window as any).__merrorSeen = true;
      }).observe(el, { childList: true, subtree: true });
      new MutationObserver((mutations) => {
        for (const m of mutations) {
          if (m.removedNodes.length > 0)
            (window as any).__renderAttempts += 1;
        }
      }).observe(el.parentElement!, { childList: true });
    });

    // type a trailing subscript, making the expression incomplete
    // ("Missing superscript or subscript argument")
    await moveCursorTo(page, 5, 24);
    await page.keyboard.type('_');

    // wait for the resulting background re-render to complete, then verify
    // the previous render was kept and no error output was ever shown
    await expect
      .poll(async () => await page.evaluate(() => (window as any).__renderAttempts), {
        timeout: 30000,
      })
      .toBeGreaterThan(0);

    expect(await page.evaluate(() => (window as any).__merrorSeen)).toBe(false);
    await expect(popupMath).toBeVisible();
    await expect(popupMath).not.toContainText('Missing');

    await sourceActions.closeSourceAndDeleteFile(fileName);
  });
});
