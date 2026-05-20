import type { Page } from 'playwright';
import { PageObject } from './page_object_base_classes';

export interface AcePosition {
  row: number;
  column: number;
}

export interface AceRange {
  start: AcePosition;
  end: AcePosition;
}

export interface AceToken {
  type: string;
  value: string;
  column?: number;
  bg?: string;
}

export interface AceMarker {
  range: AceRange | null;
  type: string;
  clazz: string;
}

/**
 * Drives an Ace editor instance from Playwright via page.evaluate.
 *
 * Identifies the target editor by a content marker -- a substring guaranteed
 * to appear in its current value. Editors inside #rstudio_console_input are
 * skipped so the source editor is found even when the console is focused.
 */
export class AceEditor extends PageObject {
  private readonly marker: string;

  constructor(page: Page, marker: string) {
    super(page);
    this.marker = marker;
  }

  /**
   * Reconstructs `fn` in the browser context, locates the editor whose value
   * contains the marker, and invokes `fn(editor, ...args)`. Args must be
   * structured-clone serializable.
   */
  private async runOnEditor<TArgs extends unknown[], TResult>(
    fn: (editor: unknown, ...args: TArgs) => TResult,
    ...args: TArgs
  ): Promise<TResult> {
    return this.page.evaluate(
      ({ marker, fnSource, args }) => {
        const editors = document.querySelectorAll('.ace_editor');
        for (let i = 0; i < editors.length; i++) {
          if (editors[i].closest('#rstudio_console_input')) continue;
          const env = (editors[i] as unknown as { env?: { editor?: { getValue(): string } } }).env;
          if (env?.editor && env.editor.getValue().indexOf(marker) !== -1) {
            // Reconstruct the function passed by the test from its source.
            // Input is trusted: it comes from our own .toString() in the caller.
            const op = (0, eval)('(' + fnSource + ')') as (editor: unknown, ...args: unknown[]) => unknown;
            return op(env.editor, ...args);
          }
        }
        throw new Error('No Ace editor found containing marker: ' + marker);
      },
      { marker: this.marker, fnSource: fn.toString(), args: args as unknown[] }
    ) as Promise<TResult>;
  }

  async getValue(): Promise<string> {
    return this.runOnEditor((editor) => (editor as { getValue(): string }).getValue());
  }

  async gotoLine(line: number, column = 0): Promise<void> {
    await this.runOnEditor(
      (editor, l: number, c: number) => (editor as { gotoLine(l: number, c: number): void }).gotoLine(l, c),
      line, column
    );
  }

  /** Returns "start", "end", or "" depending on whether the row has a fold widget. */
  async getFoldWidget(row: number): Promise<string> {
    return this.runOnEditor(
      (editor, r: number) => (editor as { session: { getFoldWidget(r: number): string } }).session.getFoldWidget(r),
      row
    );
  }

  async getFoldWidgetRange(row: number): Promise<AceRange | null> {
    return this.runOnEditor((editor, r: number) => {
      const range = (editor as { session: { getFoldWidgetRange(r: number): AceRange | null } })
        .session.getFoldWidgetRange(r);
      if (!range) return null;
      return {
        start: { row: range.start.row, column: range.start.column },
        end: { row: range.end.row, column: range.end.column },
      };
    }, row);
  }

  async getTokens(row: number): Promise<AceToken[]> {
    return this.runOnEditor(
      (editor, r: number) => (editor as { session: { getTokens(r: number): AceToken[] } }).session.getTokens(r),
      row
    );
  }

  async getTokenAt(row: number, column: number): Promise<AceToken | null> {
    return this.runOnEditor(
      (editor, r: number, c: number) =>
        (editor as { session: { getTokenAt(r: number, c: number): AceToken | null } }).session.getTokenAt(r, c),
      row, column
    );
  }

  /** Returns all Ace markers on the session, normalized to plain objects. */
  async getMarkers(): Promise<AceMarker[]> {
    return this.runOnEditor((editor) => {
      const session = (editor as { session: { getMarkers(): Record<string, AceMarker> } }).session;
      const markers = session.getMarkers();
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
    return this.runOnEditor(
      (editor, r: number) => (editor as { session: { getState(r: number): string } }).session.getState(r),
      row
    );
  }

  /**
   * Returns the editor's multi-cursor selection ranges, normalized to plain
   * objects. Useful for verifying commands like renameInScope, which place a
   * cursor on every matching occurrence.
   */
  async getSelectionRanges(): Promise<AceRange[]> {
    return this.runOnEditor((editor) => {
      const ranges = (editor as { selection: { rangeList: { ranges: AceRange[] } } })
        .selection.rangeList.ranges;
      return ranges.map((r) => ({
        start: { row: r.start.row, column: r.start.column },
        end: { row: r.end.row, column: r.end.column },
      }));
    });
  }

  async getCursorPosition(): Promise<AcePosition> {
    return this.runOnEditor((editor) => {
      const pos = (editor as { getCursorPosition(): AcePosition }).getCursorPosition();
      return { row: pos.row, column: pos.column };
    });
  }

  /** Equivalent to Ace's editor.find(needle): selects the first match and scrolls to it. */
  async find(needle: string): Promise<void> {
    await this.runOnEditor(
      (editor, n: string) => (editor as { find(n: string): void }).find(n),
      needle
    );
  }

  /**
   * Insert text at the current cursor position (Ace's editor.insert).
   * Useful when typed-key delivery is hard to time (e.g. right after a save).
   */
  async insert(text: string): Promise<void> {
    await this.runOnEditor(
      (editor, t: string) => (editor as { insert(t: string): void }).insert(t),
      text
    );
  }

  /** Move cursor to the end of the current line (Ace's editor.navigateLineEnd). */
  async navigateLineEnd(): Promise<void> {
    await this.runOnEditor((editor) => (editor as { navigateLineEnd(): void }).navigateLineEnd());
  }

  /** Move focus to the editor textarea so subsequent page.keyboard input routes here. */
  async focus(): Promise<void> {
    await this.runOnEditor((editor) => (editor as { focus(): void }).focus());
  }
}
