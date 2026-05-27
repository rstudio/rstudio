import type { Page } from 'playwright';
import { PageObject } from './page_object_base_classes';
import { Ace, AceEditorElement } from '../utils/ace';

export interface AceToken {
  type: string;
  value: string;
  column?: number;
  bg?: string;
  // True when the token is rendered by RStudio's edit-suggestion machinery
  // (ghost text, insertion preview) rather than the document's own contents.
  synthetic?: boolean;
}

export interface AceMarker {
  range: Ace.Range | null;
  type: string;
  clazz: string;
}

/**
 * Drives an Ace editor instance from Playwright via page.evaluate.
 *
 * Two ways to identify the target editor:
 *
 *   - Empty marker (`new AceEditor(page, '')`): the active source editor,
 *     resolved through `window.rstudio.documents.activeEditor()`. Prefer this
 *     when there's only one tab open, or when "the editor the user is looking
 *     at" is what the test means.
 *   - Non-empty marker: a `.ace_editor` element whose current value contains
 *     the marker substring. Use this to target a *non-active* tab (e.g. a
 *     hidden buffer left open in another tab). Editors inside
 *     #rstudio_console_input are skipped so the source editor is still found
 *     when the console happens to come first in DOM order.
 *
 * The marker-substring path is a DOM walk and can land on stale editors left
 * in the DOM after a tab close (see ad175dccd1 / #17775 and #17784). Empty
 * marker avoids that entirely.
 */
export class AceEditor extends PageObject {
  private readonly marker: string;

  constructor(page: Page, marker: string) {
    super(page);
    this.marker = marker;
  }

  /**
   * Reconstructs `fn` in the browser context, locates the editor (active doc
   * when marker is empty, marker-substring match otherwise), and invokes
   * `fn(editor, ...args)`. Args must be structured-clone serializable.
   */
  private async runOnEditor<TArgs extends unknown[], TResult>(
    fn: (editor: Ace.Editor, ...args: TArgs) => TResult,
    ...args: TArgs
  ): Promise<TResult> {
    return this.page.evaluate(
      ({ marker, fnSource, args }) => {
        // Reconstruct the function passed by the test from its source.
        // Input is trusted: it comes from our own .toString() in the caller.
        const op = (0, eval)('(' + fnSource + ')') as (editor: Ace.Editor, ...args: unknown[]) => unknown;

        if (marker === '') {
          const editor = window.rstudio?.documents.activeEditor() ?? null;
          if (!editor) {
            throw new Error(
              'AceEditor(\'\'): no active source editor (window.rstudio.documents.activeEditor() returned null)',
            );
          }
          return op(editor, ...args);
        }

        const editors = document.querySelectorAll('.ace_editor');
        for (let i = 0; i < editors.length; i++) {
          if (editors[i].closest('#rstudio_console_input')) continue;
          const env = (editors[i] as unknown as AceEditorElement).env;
          if (env?.editor && env.editor.getValue().indexOf(marker) !== -1) {
            return op(env.editor, ...args);
          }
        }
        throw new Error('No Ace editor found containing marker: ' + marker);
      },
      { marker: this.marker, fnSource: fn.toString(), args: args as unknown[] }
    ) as Promise<TResult>;
  }

  async getValue(): Promise<string> {
    return this.runOnEditor((editor) => editor.getValue());
  }

  async gotoLine(line: number, column = 0): Promise<void> {
    await this.runOnEditor(
      (editor, l: number, c: number) => editor.gotoLine(l, c),
      line, column
    );
  }

  /** Returns "start", "end", or "" depending on whether the row has a fold widget. */
  async getFoldWidget(row: number): Promise<string> {
    return this.runOnEditor((editor, r: number) => editor.session.getFoldWidget(r), row);
  }

  async getFoldWidgetRange(row: number): Promise<Ace.Range | null> {
    return this.runOnEditor((editor, r: number) => {
      const range = editor.session.getFoldWidgetRange(r);
      if (!range) return null;
      return {
        start: { row: range.start.row, column: range.start.column },
        end: { row: range.end.row, column: range.end.column },
      };
    }, row);
  }

  /** Returns the raw text of `row` (0-indexed), excluding the trailing newline. */
  async getLine(row: number): Promise<string> {
    return this.runOnEditor((editor, r: number) => editor.session.getLine(r), row);
  }

  async getTokens(row: number): Promise<AceToken[]> {
    return this.runOnEditor(
      (editor, r: number) => editor.session.getTokens(r) as AceToken[],
      row
    );
  }

  async getTokenAt(row: number, column: number): Promise<AceToken | null> {
    return this.runOnEditor(
      (editor, r: number, c: number) => editor.session.getTokenAt(r, c) as AceToken | null,
      row, column
    );
  }

  /** Returns all Ace markers on the session, normalized to plain objects. */
  async getMarkers(): Promise<AceMarker[]> {
    return this.runOnEditor((editor) => {
      const markers = editor.session.getMarkers() as Record<string, AceMarker>;
      return Object.values(markers).map((m) => ({
        range: m.range
          ? {
              start: { row: m.range.start.row, column: m.range.start.column },
              end: { row: m.range.end.row, column: m.range.end.column },
            }
          : null,
        type: m.type,
        clazz: m.clazz,
      }));
    });
  }

  async getState(row: number): Promise<string> {
    return this.runOnEditor((editor, r: number) => editor.session.getState(r), row);
  }

  /**
   * Returns the editor's multi-cursor selection ranges, normalized to plain
   * objects. Useful for verifying commands like renameInScope, which place a
   * cursor on every matching occurrence.
   */
  async getSelectionRanges(): Promise<Ace.Range[]> {
    return this.runOnEditor((editor) => {
      const ranges = editor.selection.rangeList.ranges;
      return ranges.map((r) => ({
        start: { row: r.start.row, column: r.start.column },
        end: { row: r.end.row, column: r.end.column },
      }));
    });
  }

  async getCursorPosition(): Promise<Ace.Position> {
    return this.runOnEditor((editor) => {
      const pos = editor.getCursorPosition();
      return { row: pos.row, column: pos.column };
    });
  }

  /** Equivalent to Ace's editor.find(needle): selects the first match and scrolls to it. */
  async find(needle: string): Promise<void> {
    await this.runOnEditor((editor, n: string) => editor.find(n), needle);
  }

  /**
   * Insert text at the current cursor position (Ace's editor.insert).
   * Useful when typed-key delivery is hard to time (e.g. right after a save).
   */
  async insert(text: string): Promise<void> {
    await this.runOnEditor((editor, t: string) => editor.insert(t), text);
  }

  /** Move cursor to the end of the current line (Ace's editor.navigateLineEnd). */
  async navigateLineEnd(): Promise<void> {
    await this.runOnEditor((editor) => editor.navigateLineEnd());
  }

  /** Move focus to the editor textarea so subsequent page.keyboard input routes here. */
  async focus(): Promise<void> {
    await this.runOnEditor((editor) => editor.focus());
  }
}
