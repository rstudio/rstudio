import type { Page } from 'playwright';
import { sleep } from '../utils/constants';
import { ConsolePaneActions } from './console_pane.actions';
import { SourcePaneActions } from './source_pane.actions';

export const COMPLETION_POPUP = '#rstudio_popup_completions';

export class AutocompleteActions {
  readonly page: Page;
  readonly consoleActions: ConsolePaneActions;
  readonly sourceActions: SourcePaneActions;

  constructor(page: Page, consoleActions: ConsolePaneActions, sourceActions: SourcePaneActions) {
    this.page = page;
    this.consoleActions = consoleActions;
    this.sourceActions = sourceActions;
  }

  /**
   * Wait for the autocomplete popup and return completion item names.
   * Extracts the display name from the first span in each grid cell,
   * skipping the icon and any package/source/argument spans.
   */
  async getCompletionItems(timeoutMs = 5000): Promise<string[]> {
    await this.page.locator(COMPLETION_POPUP).waitFor({ state: 'visible', timeout: timeoutMs });
    await sleep(500);

    return await this.page.evaluate(`(function() {
      var popup = document.getElementById('rstudio_popup_completions');
      if (!popup) return [];
      var cells = popup.querySelectorAll('td');
      var names = [];
      for (var i = 0; i < cells.length; i++) {
        var spans = cells[i].querySelectorAll('span');
        if (spans.length > 0) {
          var name = spans[0].textContent.trim();
          if (name) names.push(name);
        }
      }
      return names;
    })()`);
  }

  /** Dismiss the autocomplete popup. */
  async dismiss(): Promise<void> {
    await this.page.keyboard.press('Escape');
    await sleep(300);
  }

  /**
   * Get completions in the console.
   * Executes setupCode, then types triggerText (without Enter) and presses Ctrl+Space.
   */
  async getCompletionsInConsole(setupCode: string[], triggerText: string): Promise<string[]> {
    for (const code of setupCode) {
      await this.consoleActions.typeInConsole(code);
      await sleep(1000);
    }

    // Type trigger text without executing
    await this.consoleActions.consolePane.consoleInput.click({ force: true });
    await sleep(300);
    await this.consoleActions.consolePane.consoleInput.pressSequentially(triggerText);
    await sleep(500);

    // If autocomplete already appeared (e.g. after typing $ or (), use it;
    // otherwise trigger explicitly with Ctrl+Space
    const popupAlreadyVisible = await this.page.locator(COMPLETION_POPUP).isVisible();
    if (!popupAlreadyVisible) {
      await this.page.keyboard.press('Control+Space');
    }
    const items = await this.getCompletionItems();

    // Cleanup: dismiss popup, cancel partial input
    await this.dismiss();
    await this.page.keyboard.press('Escape');
    await sleep(300);

    return items;
  }

  /**
   * Get completions in the editor.
   * Executes setupCode in console, creates a temp .R file with fileContent,
   * positions cursor at cursorLine/cursorCol (or end of content), presses Ctrl+Space.
   */
  async getCompletionsInEditor(
    setupCode: string[],
    fileContent: string,
    cursorLine?: number,
    cursorCol?: number,
  ): Promise<string[]> {
    for (const code of setupCode) {
      await this.consoleActions.typeInConsole(code);
      await sleep(1000);
    }

    const fileName = `ac_test_${Date.now()}.R`;
    await this.sourceActions.createAndOpenFile(fileName, fileContent);
    await sleep(1000);

    // Ensure the editor has DOM-level focus for keyboard events
    await this.sourceActions.sourcePane.aceTextInput.click({ force: true });
    await sleep(300);

    // Position cursor in the source editor (skip console editor)
    if (cursorLine !== undefined && cursorCol !== undefined) {
      await this.page.evaluate(`
        var editors = document.querySelectorAll('.ace_editor');
        for (var i = 0; i < editors.length; i++) {
          if (editors[i].closest('#rstudio_console_input')) continue;
          var env = editors[i].env;
          if (env && env.editor) {
            env.editor.focus();
            env.editor.gotoLine(${cursorLine}, ${cursorCol});
            break;
          }
        }
      `);
    } else {
      // Default: end of last non-empty line
      await this.page.evaluate(`
        var editors = document.querySelectorAll('.ace_editor');
        for (var i = 0; i < editors.length; i++) {
          if (editors[i].closest('#rstudio_console_input')) continue;
          var env = editors[i].env;
          if (env && env.editor) {
            var editor = env.editor;
            editor.focus();
            var lastRow = editor.session.getLength() - 1;
            while (lastRow > 0 && editor.session.getLine(lastRow).trim() === '') lastRow--;
            var lastCol = editor.session.getLine(lastRow).length;
            editor.gotoLine(lastRow + 1, lastCol);
            break;
          }
        }
      `);
    }
    await sleep(500);

    // If autocomplete already appeared (e.g. after typing $ or (), use it;
    // otherwise trigger explicitly with Ctrl+Space
    const popupAlreadyVisible = await this.page.locator(COMPLETION_POPUP).isVisible();
    if (!popupAlreadyVisible) {
      await this.page.keyboard.press('Control+Space');
    }
    const items = await this.getCompletionItems();

    // Cleanup: dismiss popup, close and delete file
    await this.dismiss();
    await this.sourceActions.closeSourceAndDeleteFile(fileName);

    return items;
  }
}
