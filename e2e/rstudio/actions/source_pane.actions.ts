import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { SourcePane } from '../pages/source_pane.page';
import { ConsolePaneActions } from './console_pane.actions';
import { clickConfirmIfVisible } from '../pages/modals.page';
import { TIMEOUTS, sleep } from '../utils/constants';
import { rStringLiteral } from '../utils/r';
import { executeCommand, dismissAllModals } from '../utils/commands';
import { openFile } from '../utils/files';
import { Ace, AceEditorElement } from '../utils/ace';

export class SourcePaneActions {
  readonly page: Page;
  readonly sourcePane: SourcePane;
  readonly consolePaneActions: ConsolePaneActions;

  constructor(page: Page, consolePaneActions: ConsolePaneActions) {
    this.page = page;
    this.sourcePane = new SourcePane(page);
    this.consolePaneActions = consolePaneActions;
  }

  /**
   * Write `fileContent` to `fileName` in R's current working directory and
   * open it in the editor. Pass real newlines and unescaped quotes -- the
   * helper routes the content through `rStringLiteral` (JSON-stringify) so
   * R receives it intact. Earlier callers had to pre-escape `\n` to `\\n`
   * because the previous implementation interpolated the content into a
   * plain `writeLines("...", ...)` template; that gotcha is gone.
   */
  async createAndOpenFile(fileName: string, fileContent: string): Promise<void> {
    // wait: true ensures the writeLines completes before file.edit() reads
    // the file we just wrote (polls the busy class, no blind sleep).
    await this.consolePaneActions.executeInConsole(
      `writeLines(${rStringLiteral(fileContent)}, ${rStringLiteral(fileName)})`,
      { wait: true },
    );

    await openFile(this.page, fileName);
  }

  async closeSourceAndDeleteFile(fileName: string): Promise<void> {
    await executeCommand(this.page, 'saveAllSourceDocs');
    await sleep(1000);
    // resetSourcePane closes every tab except a single Untitled placeholder,
    // so the source pane never transits through the zero-tab HIDE state
    // (#17738) and the next test starts from a known good state. The previous
    // closeAllSourceDocs + toHaveCount(0) wait left the pane briefly empty,
    // and the auto-spawned Untitled1 that filled the gap collided with the
    // new file the next test opened (two publishBtns -> strict-mode error).
    await this.consolePaneActions.resetSourcePane();
    await this.consolePaneActions.executeInConsole(`unlink("${fileName}")`, { wait: true });
  }

  async sendText(text: string): Promise<void> {
    await this.sourcePane.contentPane.click();
    await this.page.keyboard.type(text);
  }

  async acceptNesRename(): Promise<string> {
    const { nesApply, ghostText, nesInsertionPreview, nesGutter, nesSuggestionContent } = this.sourcePane;

    await expect(nesApply.or(ghostText).or(nesInsertionPreview).or(nesGutter).first()).toBeVisible({ timeout: TIMEOUTS.nesApply });

    if (await nesApply.first().isVisible()) {
      const nesSuggestion = nesSuggestionContent.first();
      await expect(nesSuggestion).toBeVisible();
      const nesSuggestionText = await nesSuggestion.textContent();
      const nesSuggestionHtml = await nesSuggestion.innerHTML();
      console.log('  NES Apply suggestion (text): ' + nesSuggestionText);
      console.log('  NES Apply suggestion (html): ' + nesSuggestionHtml);
      await nesApply.first().click();
      return 'apply';
    } else if (await nesInsertionPreview.first().isVisible()) {
      const insertionText = await nesInsertionPreview.first().textContent();
      const count = await nesInsertionPreview.count();
      console.log(`  NES inline diff: "${insertionText}" (${count} suggestion(s) visible)`);
      await this.page.keyboard.press('ControlOrMeta+;');
      return 'inline-diff';
    } else if (await ghostText.first().isVisible()) {
      const ghostTextParts = await ghostText.allTextContents();
      console.log('  NES ghost text: "' + ghostTextParts.join('') + '"');
      await this.page.keyboard.press('ControlOrMeta+;');
      return 'ghost-text';
    } else {
      console.log('  NES gutter icon detected — clicking to reveal suggestion...');
      await nesGutter.first().click();

      try {
        await expect(nesApply.or(ghostText).or(nesInsertionPreview).first()).toBeVisible({ timeout: 15000 });

        if (await nesApply.first().isVisible()) {
          const nesSuggestionText = await nesSuggestionContent.first().textContent();
          console.log('  NES Apply suggestion (after gutter click): ' + nesSuggestionText);
          await nesApply.first().click();
        } else if (await nesInsertionPreview.first().isVisible()) {
          const insertionText = await nesInsertionPreview.first().textContent();
          console.log('  NES inline diff (after gutter click): "' + insertionText + '"');
          await this.page.keyboard.press('ControlOrMeta+;');
        } else {
          const ghostParts = await ghostText.allTextContents();
          console.log('  NES ghost text (after gutter click): "' + ghostParts.join('') + '"');
          await this.page.keyboard.press('ControlOrMeta+;');
        }
        return 'gutter-clicked';
      } catch {
        console.log('  WARNING: No suggestion appeared after clicking gutter icon');
        throw new Error('NES gutter icon clicked but no suggestion appeared');
      }
    }
  }

  /** Move cursor to end of document (cross-platform). */
  async goToEnd(): Promise<void> {
    await this.sourcePane.aceTextInput.click({ force: true });
    const goToEnd = process.platform === 'darwin' ? 'Meta+ArrowDown' : 'Control+End';
    await this.page.keyboard.press(goToEnd);
  }

  /** Move cursor to top of document (cross-platform). */
  async goToTop(): Promise<void> {
    await this.sourcePane.aceTextInput.click({ force: true });
    const goToTop = process.platform === 'darwin' ? 'Meta+ArrowUp' : 'Control+Home';
    await this.page.keyboard.press(goToTop);
  }

  /**
   * Navigate to a chunk by its 1-based index.
   * Clicks into the editor, goes to the top of the document,
   * then calls goToNextChunk N times.
   */
  async navigateToChunkByIndex(chunkNumber: number): Promise<void> {
    await this.goToTop();
    for (let i = 0; i < chunkNumber; i++) {
      await executeCommand(this.page, 'goToNextChunk');
      await sleep(500);
    }
  }

  /**
   * Navigate to a chunk by its label (e.g., 'slow_plot' for ```{r slow_plot}).
   * Uses the Ace editor API to find the chunk header and position the cursor there.
   */
  async navigateToChunkByLabel(label: string): Promise<void> {
    await this.sourcePane.aceTextInput.click({ force: true });
    await this.page.evaluate((lbl) => {
      const editors = document.querySelectorAll('.ace_editor');
      for (let i = 0; i < editors.length; i++) {
        if (editors[i].closest('#rstudio_console_input')) continue;
        const editor = (editors[i] as unknown as AceEditorElement).env?.editor;
        if (!editor) continue;
        const lines = editor.getValue().split('\n');
        const pattern = new RegExp('```\\{r\\s+' + lbl + '[\\s,}]');
        for (let j = 0; j < lines.length; j++) {
          if (pattern.test(lines[j])) {
            editor.gotoLine(j + 1, 0);
            return;
          }
        }
      }
    }, label);
  }

  /**
   * Select text in the active editor using Ace's API.
   * Finds the editor whose content contains `marker`, moves to `line`,
   * and selects from column `startCol` to `endCol`.
   */
  async selectInEditor(marker: string, line: number, startCol: number, endCol: number): Promise<void> {
    await this.page.evaluate(({ marker: m, line: ln, startCol: sc, endCol: ec }) => {
      const editors = document.querySelectorAll('.ace_editor');
      for (let i = 0; i < editors.length; i++) {
        const editor = (editors[i] as unknown as AceEditorElement).env?.editor;
        if (!editor) continue;
        if (editor.getValue().indexOf(m) === -1) continue;
        editor.focus();
        editor.gotoLine(ln, 0);
        // Ace's Range constructor lives on the global ace module loader; we
        // grab it here rather than in utils/ace.ts because it's the only
        // construction site and the module-loader surface isn't worth typing.
        const aceModule = (window as unknown as {
          ace: { require(path: string): { Range: new (sr: number, sc: number, er: number, ec: number) => Ace.Range } };
        }).ace;
        const Range = aceModule.require('ace/range').Range;
        editor.selection.setRange(new Range(ln - 1, sc, ln - 1, ec));
        return;
      }
    }, { marker, line, startCol, endCol });
    await sleep(1000);
  }

  /**
   * Get the full text content of the active source editor via the
   * automation bridge. `window.rstudio.documents.activeEditor()` returns
   * the same native Ace instance the GWT side already tracks as "the
   * active doc," so there's no DOM scan and no chance of landing on the
   * console scroll panel, dialog editors, or stale source editors that
   * linger in the DOM after a tab close.
   */
  async getEditorContent(): Promise<string> {
    return await this.page.evaluate(() => {
      const editor = window.rstudio?.documents.activeEditor() ?? null;
      if (!editor) throw new Error('No active source editor');
      return editor.getValue();
    });
  }

  async getSelectedText(marker: string): Promise<string> {
    return await this.page.evaluate((m) => {
      const editors = document.querySelectorAll('.ace_editor');
      for (let i = 0; i < editors.length; i++) {
        const editor = (editors[i] as unknown as AceEditorElement).env?.editor;
        if (editor && editor.getValue().indexOf(m) !== -1) {
          return editor.getSelectedText();
        }
      }
      return '';
    }, marker);
  }

  /**
   * Get the first visible row index (0-based) of the active source editor.
   * Uses the automation bridge's activeEditor() rather than scanning
   * `.ace_editor` nodes -- a DOM scan can land on a stale or background
   * editor (e.g. the empty Untitled tab the suite keeps open), which sorts
   * first and always reports row 0. See getEditorContent for the same pattern.
   */
  async getFirstVisibleRow(): Promise<number> {
    return await this.page.evaluate(() => {
      const editor = window.rstudio?.documents.activeEditor() ?? null;
      if (!editor) throw new Error('No active source editor');
      return editor.getFirstVisibleRow();
    });
  }

  /**
   * Ensures the editor is in visual mode. If already in visual mode, does nothing.
   */
  async ensureVisualMode(): Promise<void> {
    const toggle = this.sourcePane.visualMdToggle;
    const proseMirror = this.page.locator('.ProseMirror');

    // Already in visual mode? The editor mounts a .ProseMirror surface.
    if (await proseMirror.first().isVisible().catch(() => false)) return;

    try {
      // Wait for the toggle to mount -- a freshly created template document
      // takes a moment before its editor toolbar is ready, and a 3s attribute
      // read could race that. If it never appears, visual mode isn't supported.
      await toggle.waitFor({ state: 'visible', timeout: 5000 });
    } catch {
      // Toggle not available -- visual mode not supported for this file type.
      return;
    }

    // Bring the source pane to the foreground before toggling. Per-test reset
    // activates Environment + Files panes (added in #17950); on macOS 26 the
    // visual toggle click otherwise races a delayed focus shift and panmirror
    // never finishes mounting. Clicking the open tab is a cheap focus signal.
    await this.sourcePane.selectedTab.click({ timeout: 2000 }).catch(() => {});

    await toggle.click();
    // The first switch to visual mode for a document can raise a confirmation.
    await clickConfirmIfVisible(this.page, 5000);
    // Sweep any other modal that the toggle may have stacked underneath the
    // confirmation (e.g. a "save changes?" or "convert to visual" follow-up).
    // dismissAll is a no-op when nothing is showing.
    await dismissAllModals(this.page).catch(() => {});
    // Wait for the visual editor to actually mount rather than sleeping a fixed
    // interval: converting a heavier document (a template with chunks/plots)
    // through pandoc takes longer than a blank one, and the old 2s sleep could
    // return before .ProseMirror existed -- leaving callers asserting against a
    // still-source-mode editor.
    await proseMirror.first().waitFor({ state: 'visible', timeout: 180000 });
  }

  /**
   * Ensures the editor is in source mode. If already in source mode, does nothing.
   */
  async ensureSourceMode(): Promise<void> {
    const toggle = this.sourcePane.visualMdToggle;
    try {
      const ariaPressed = await toggle.getAttribute('aria-pressed', { timeout: 3000 });
      if (ariaPressed === 'true') {
        await toggle.click();
        await sleep(1000);
      }
    } catch {
      // Toggle not found — already in source mode
    }
  }
}
