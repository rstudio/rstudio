import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { SourcePane } from '../pages/source_pane.page';
import { ConsolePaneActions } from './console_pane.actions';
import { clickConfirmIfVisible } from '../pages/modals.page';
import { TIMEOUTS, sleep } from '../utils/constants';

export class SourcePaneActions {
  readonly page: Page;
  readonly sourcePane: SourcePane;
  readonly consolePaneActions: ConsolePaneActions;

  constructor(page: Page, consolePaneActions: ConsolePaneActions) {
    this.page = page;
    this.sourcePane = new SourcePane(page);
    this.consolePaneActions = consolePaneActions;
  }

  async createAndOpenFile(fileName: string, fileContent: string): Promise<void> {
    await this.consolePaneActions.typeInConsole(`writeLines("${fileContent}", "${fileName}")`);
    await sleep(1000);

    await this.consolePaneActions.typeInConsole(`file.edit('${fileName}')`);

    await expect(this.sourcePane.selectedTab).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });
  }

  async closeSourceAndDeleteFile(fileName: string): Promise<void> {
    await this.consolePaneActions.typeInConsole(".rs.api.executeCommand('saveAllSourceDocs')");
    await sleep(1000);
    await this.consolePaneActions.typeInConsole(".rs.api.executeCommand('closeAllSourceDocs')");
    await sleep(1000);

    await expect(this.sourcePane.aceTextInput).toHaveCount(0, { timeout: 5000 }).catch(() => {});
    await sleep(500);

    await this.consolePaneActions.typeInConsole(`unlink("${fileName}")`);
    await sleep(500);
  }

  async sendText(text: string): Promise<void> {
    await this.sourcePane.contentPane.click();
    await sleep(300);
    await this.page.keyboard.type(text);
    await sleep(300);
  }

  async acceptNesRename(): Promise<string> {
    const { nesApply, copilotGhostText, nesInsertionPreview, nesGutter, nesSuggestionContent } = this.sourcePane;

    await expect(nesApply.or(copilotGhostText).or(nesInsertionPreview).or(nesGutter).first()).toBeVisible({ timeout: TIMEOUTS.nesApply });
    await sleep(2000);

    if (await nesApply.first().isVisible()) {
      const nesSuggestion = nesSuggestionContent.first();
      await expect(nesSuggestion).toBeVisible();
      const nesSuggestionText = await nesSuggestion.textContent();
      const nesSuggestionHtml = await nesSuggestion.innerHTML();
      console.log('  NES Apply suggestion (text): ' + nesSuggestionText);
      console.log('  NES Apply suggestion (html): ' + nesSuggestionHtml);
      await nesApply.first().click();
      await sleep(2000);
      return 'apply';
    } else if (await nesInsertionPreview.first().isVisible()) {
      const insertionText = await nesInsertionPreview.first().textContent();
      const count = await nesInsertionPreview.count();
      console.log(`  NES inline diff: "${insertionText}" (${count} suggestion(s) visible)`);
      await this.page.keyboard.press('Control+;');
      await sleep(2000);
      return 'inline-diff';
    } else if (await copilotGhostText.first().isVisible()) {
      const ghostTextParts = await copilotGhostText.allTextContents();
      console.log('  NES ghost text: "' + ghostTextParts.join('') + '"');
      await this.page.keyboard.press('Control+;');
      await sleep(2000);
      return 'ghost-text';
    } else {
      console.log('  NES gutter icon detected — clicking to reveal suggestion...');
      await nesGutter.first().click();
      await sleep(2000);

      try {
        await expect(nesApply.or(copilotGhostText).or(nesInsertionPreview).first()).toBeVisible({ timeout: 15000 });
        await sleep(2000);

        if (await nesApply.first().isVisible()) {
          const nesSuggestionText = await nesSuggestionContent.first().textContent();
          console.log('  NES Apply suggestion (after gutter click): ' + nesSuggestionText);
          await nesApply.first().click();
          await sleep(2000);
        } else if (await nesInsertionPreview.first().isVisible()) {
          const insertionText = await nesInsertionPreview.first().textContent();
          console.log('  NES inline diff (after gutter click): "' + insertionText + '"');
          await this.page.keyboard.press('Control+;');
          await sleep(2000);
        } else {
          const ghostParts = await copilotGhostText.allTextContents();
          console.log('  NES ghost text (after gutter click): "' + ghostParts.join('') + '"');
          await this.page.keyboard.press('Control+;');
          await sleep(2000);
        }
        return 'gutter-clicked';
      } catch {
        console.log('  WARNING: No suggestion appeared after clicking gutter icon');
        throw new Error('NES gutter icon clicked but no suggestion appeared');
      }
    }
  }

  /**
   * Navigate to a chunk by its 1-based index.
   * Clicks into the editor, goes to the top of the document,
   * then calls goToNextChunk N times.
   */
  async navigateToChunkByIndex(chunkNumber: number): Promise<void> {
    await this.sourcePane.aceTextInput.click({ force: true });
    await sleep(300);
    await this.page.keyboard.press('Control+Home');
    await sleep(300);
    for (let i = 0; i < chunkNumber; i++) {
      await this.consolePaneActions.typeInConsole(".rs.api.executeCommand('goToNextChunk')");
      await sleep(500);
    }
  }

  /**
   * Navigate to a chunk by its label (e.g., 'slow_plot' for ```{r slow_plot}).
   * Uses the Ace editor API to find the chunk header and position the cursor there.
   */
  async navigateToChunkByLabel(label: string): Promise<void> {
    await this.sourcePane.aceTextInput.click({ force: true });
    await sleep(300);
    await this.page.evaluate(`(function() {
      var editors = document.querySelectorAll('.ace_editor');
      for (var i = 0; i < editors.length; i++) {
        if (editors[i].closest('#rstudio_console_input')) continue;
        var env = editors[i].env;
        if (env && env.editor) {
          var editor = env.editor;
          var text = editor.getValue();
          var lines = text.split('\\n');
          for (var j = 0; j < lines.length; j++) {
            if (lines[j].match(/\`\`\`\\{r\\s+${label}[\\s,}]/)) {
              editor.gotoLine(j + 1, 0);
              return;
            }
          }
        }
      }
    })()`);
    await sleep(300);
  }

  /**
   * Select text in the active editor using Ace's API.
   * Finds the editor whose content contains `marker`, moves to `line`,
   * and selects from column `startCol` to `endCol`.
   */
  async selectInEditor(marker: string, line: number, startCol: number, endCol: number): Promise<void> {
    await this.page.evaluate(`
      var editors = document.querySelectorAll('.ace_editor');
      for (var i = 0; i < editors.length; i++) {
        var env = editors[i].env;
        if (env && env.editor) {
          var editor = env.editor;
          var text = editor.getValue();
          if (text.indexOf('${marker}') !== -1) {
            editor.focus();
            editor.gotoLine(${line}, 0);
            var Range = ace.require('ace/range').Range;
            editor.selection.setRange(new Range(${line - 1}, ${startCol}, ${line - 1}, ${endCol}));
            break;
          }
        }
      }
    `);
    await sleep(1000);
  }

  /**
   * Get the full text content of the active source editor via Ace API.
   */
  async getEditorContent(): Promise<string> {
    return await this.page.evaluate(`(function() {
      var editors = document.querySelectorAll('.ace_editor');
      for (var i = 0; i < editors.length; i++) {
        if (editors[i].closest('#rstudio_console_input')) continue;
        var env = editors[i].env;
        if (env && env.editor) {
          return env.editor.getValue();
        }
      }
      return '';
    })()`);
  }

  async getSelectedText(marker: string): Promise<string> {
    return await this.page.evaluate(`(function() {
      var editors = document.querySelectorAll('.ace_editor');
      for (var i = 0; i < editors.length; i++) {
        var env = editors[i].env;
        if (env && env.editor && env.editor.getValue().indexOf('${marker}') !== -1) {
          return env.editor.getSelectedText();
        }
      }
      return '';
    })()`);
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
