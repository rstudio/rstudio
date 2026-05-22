import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { SourcePane } from '../pages/source_pane.page';
import { ConsolePaneActions } from './console_pane.actions';
import { clickConfirmIfVisible } from '../pages/modals.page';
import { TIMEOUTS, sleep } from '../utils/constants';
import { rStringLiteral } from '../utils/r';
import { executeCommand } from '../utils/commands';
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

    await this.consolePaneActions.executeInConsole(`file.edit(${rStringLiteral(fileName)})`);

    await expect(this.sourcePane.selectedTab).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
  }

  async closeSourceAndDeleteFile(fileName: string): Promise<void> {
    await executeCommand(this.page, 'saveAllSourceDocs');
    await sleep(1000);
    await executeCommand(this.page, 'closeAllSourceDocs');

    // Wait for the source pane to be fully empty -- both no editor mounted
    // and no tab strip entry. The tab-strip check (selectedTab.count == 0)
    // is the stronger signal: an immediately-following file.edit can race
    // GWT's close cleanup and end up not opening the new tab if we only
    // wait on aceTextInput.
    await expect(this.sourcePane.aceTextInput).toHaveCount(0, { timeout: 5000 }).catch(() => {});
    await expect(this.sourcePane.selectedTab).toHaveCount(0, { timeout: 5000 }).catch(() => {});

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
   * Get the full text content of the active source editor via Ace API.
   */
  async getEditorContent(): Promise<string> {
    return await this.page.evaluate(() => {
      const editors = document.querySelectorAll('.ace_editor');
      for (let i = 0; i < editors.length; i++) {
        if (editors[i].closest('#rstudio_console_input')) continue;
        const editor = (editors[i] as unknown as AceEditorElement).env?.editor;
        if (editor) return editor.getValue();
      }
      throw new Error('No active source editor found');
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

  /** Get the first visible row index (0-based) of the active source editor. */
  async getFirstVisibleRow(): Promise<number> {
    return await this.page.evaluate(() => {
      const editors = document.querySelectorAll('.ace_editor');
      for (let i = 0; i < editors.length; i++) {
        if (editors[i].closest('#rstudio_console_input')) continue;
        const editor = (editors[i] as unknown as AceEditorElement).env?.editor;
        if (editor) return editor.getFirstVisibleRow();
      }
      throw new Error('No active source editor found');
    });
  }

  /**
   * Ensures the editor is in visual mode. If already in visual mode, does nothing.
   */
  async ensureVisualMode(): Promise<void> {
    const toggle = this.sourcePane.visualMdToggle;
    try {
      const ariaPressed = await toggle.getAttribute('aria-pressed', { timeout: 3000 });
      if (ariaPressed === 'false') {
        await toggle.click();
        await clickConfirmIfVisible(this.page, 5000);
        await sleep(2000);
      }
    } catch {
      // Toggle not available — visual mode not supported for this file type
    }
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
