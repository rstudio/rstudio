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
  async getCompletionItems(timeoutMs = 15000): Promise<string[]> {
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
      await this.consoleActions.executeInConsole(code);
      await sleep(1000);
    }

    // Type trigger text without executing -- needs per-key events to fire the completer.
    await this.consoleActions.typeInConsole(triggerText);
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
   * Regression coverage for #17831. Types `pkg:::` in the console to bring up
   * the all-objects completion popup, reads the items, then presses Backspace
   * once (turning `:::` into `::`) and reads the items again. Deleting the
   * colon changes the namespace context, so the list should narrow from all
   * objects to only the exported ones.
   *
   * Returns both lists. `exported` is whatever the popup settled on after the
   * backspace; if the fix is absent it stays equal to `all`, so the caller's
   * assertion fails clearly rather than relying on a generic wait timeout.
   */
  async getNamespaceCompletionsBeforeAndAfterBackspace(
    pkg: string,
  ): Promise<{ all: string[]; exported: string[] }> {
    await this.consoleActions.typeInConsole(`${pkg}:::`);
    await this.page.locator(COMPLETION_POPUP).waitFor({ state: 'visible', timeout: 15000 });
    const all = await this.getCompletionItems();

    // Delete the trailing colon: ':::' -> '::'. This re-queries the server,
    // which should narrow the list from all objects to only exported ones.
    await this.page.keyboard.press('Backspace');

    // Poll until the popup shrinks (the fix landed) or the deadline passes
    // (the bug is present and the count never changes).
    let exported = all;
    const deadline = Date.now() + 10000;
    while (Date.now() < deadline) {
      const current = await this.getCompletionItems();
      exported = current;
      if (current.length < all.length) break;
    }

    // Cleanup: dismiss popup, cancel partial input
    await this.dismiss();
    await this.page.keyboard.press('Escape');
    await sleep(300);

    return { all, exported };
  }

  /**
   * Get completions in the editor.
   * Executes setupCode in console, creates a temp file (default extension `R`)
   * with fileContent, positions cursor at cursorLine/cursorCol (or end of
   * content), presses Ctrl+Space.
   */
  async getCompletionsInEditor(
    setupCode: string[],
    fileContent: string,
    cursorLine?: number,
    cursorCol?: number,
    extension: string = 'R',
  ): Promise<string[]> {
    for (const code of setupCode) {
      await this.consoleActions.executeInConsole(code);
      await sleep(1000);
    }

    const fileName = `ac_test_${Date.now()}.${extension}`;
    await this.sourceActions.createAndOpenFile(fileName, fileContent);
    await sleep(1000);

    // Ensure the editor has DOM-level focus for keyboard events
    await this.sourceActions.sourcePane.aceTextInput.click({ force: true });
    await sleep(300);

    // Position cursor in the source editor via the automation bridge.
    // A DOM scan with `.ace_editor` here would pick stale editors left in
    // the DOM after a tab close (ad175dccd1 / #17775) -- the chosen empty
    // editor would silently receive focus + gotoLine, and Ctrl+Space then
    // fires against an editor with no content to complete on.
    await this.page.evaluate(
      ({ line, col }: { line: number | null; col: number | null }) => {
        const editor = window.rstudio?.documents.activeEditor() ?? null;
        if (!editor) throw new Error('No active source editor');
        editor.focus();
        if (line !== null && col !== null) {
          editor.gotoLine(line, col);
        } else {
          // Default: end of last non-empty line.
          const session = editor.session;
          let lastRow = session.getLength() - 1;
          while (lastRow > 0 && session.getLine(lastRow).trim() === '') lastRow--;
          const lastCol = session.getLine(lastRow).length;
          editor.gotoLine(lastRow + 1, lastCol);
        }
      },
      { line: cursorLine ?? null, col: cursorCol ?? null },
    );
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
