// resetToUntitled must hand back a CLEAN untitled placeholder.
//
// The placeholder is shared state: resetSourcePaneState keeps one Untitled tab
// open so the source pane never transits the zero-tab HIDE state (#17738), and
// every spec in a worker inherits whatever the previous one left in it. Before
// this was fixed, SourceColumnManager reused the existing untitled doc without
// looking at its dirty flag -- revertUnsavedTargets only reverts file-backed
// editors -- so a spec that typed into the placeholder (e.g. ggplot_pipe_insert
// setting editor content directly) left that text behind. The next spec to save
// all documents then got a modal Save File prompt for the pathless doc, and the
// prompt's glass panel blocked every subsequent click: the markdown
// HTML-preview spec failed this way on all four platforms at once, with the
// misleading symptom "console input did not hold focus".

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';

test.describe('Source pane reset', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    await consoleActions.resetSourcePane();
  });

  test('a dirty Untitled placeholder is replaced, not reused', async ({ rstudioPage: page }) => {
    // Dirty the placeholder the way a spec would: write into its editor and
    // leave it unsaved.
    const editor = new AceEditor(page, '');
    await editor.setValue('ggplot(mtcars, aes(wt, mpg)) + geom_point()');
    await expect
      .poll(() => page.evaluate(() => window.rstudio?.documents.active()?.dirty ?? null))
      .toBe(true);

    await consoleActions.resetSourcePane();

    // Still a single untitled tab -- the replacement must not leave the dirty
    // one behind, and must not empty the pane on the way through.
    const tabs = page.locator("[class*='rstudio_source_panel'] [role='tab']");
    await expect(tabs).toHaveCount(1);

    const active = await page.evaluate(() => window.rstudio?.documents.active() ?? null);
    expect(active).not.toBeNull();
    expect(active?.path).toBeNull();
    expect(active?.dirty).toBe(false);
    expect(await editor.getValue()).toBe('');
  });
});
