import type { Page, JSHandle } from 'playwright';
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
   * Resolve the target editor in the browser and return a JSHandle to it.
   * Pairs with `run()` below, which disposes the handle when done.
   */
  private editorHandle(): Promise<JSHandle<Ace.Editor>> {
    return this.page.evaluateHandle((marker: string): Ace.Editor => {
      if (marker === '') {
        const editor = window.rstudio?.documents.activeEditor() ?? null;
        if (!editor) {
          throw new Error(
            'AceEditor(\'\'): no active source editor (window.rstudio.documents.activeEditor() returned null)',
          );
        }
        return editor;
      }
      const editors = document.querySelectorAll('.ace_editor');
      for (let i = 0; i < editors.length; i++) {
        if (editors[i].closest('#rstudio_console_input')) continue;
        const env = (editors[i] as unknown as AceEditorElement).env;
        if (env?.editor && env.editor.getValue().indexOf(marker) !== -1) {
          return env.editor;
        }
      }
      throw new Error('No Ace editor found containing marker: ' + marker);
    }, this.marker);
  }

  /**
   * Resolve the editor, run `fn(editor, arg)` against it in the browser, and
   * return the result. Playwright serializes `fn` natively, so closures over
   * Node-side state don't work -- pass everything through `arg`, which must
   * be structured-clone serializable.
   */
  private async run<R>(fn: (editor: Ace.Editor) => R): Promise<R>;
  private async run<A, R>(fn: (editor: Ace.Editor, arg: A) => R, arg: A): Promise<R>;
  private async run<A, R>(
    fn: ((editor: Ace.Editor) => R) | ((editor: Ace.Editor, arg: A) => R),
    arg?: A,
  ): Promise<R> {
    const handle = await this.editorHandle();
    try {
      // Playwright's PageFunctionOn types wrap the arg in Unboxed<A>, which
      // can't be reconciled with our open A from the public overloads. The
      // overloads above keep call sites type-safe; the cast here just lets
      // the bridge call through.
      const handleEvaluate = handle.evaluate.bind(handle) as (
        f: typeof fn,
        a?: A,
      ) => Promise<R>;
      return arguments.length === 1 ? await handleEvaluate(fn) : await handleEvaluate(fn, arg);
    } finally {
      await handle.dispose();
    }
  }

  async getValue(): Promise<string> {
    return this.run((editor) => editor.getValue());
  }

  /**
   * Moves the cursor to `line` (1-indexed, matching Ace's own gotoLine and the
   * editor's gutter), distinct from the 0-indexed row taken by getLine et al.
   */
  async gotoLine(line: number, column = 0): Promise<void> {
    await this.run(
      (editor, pos: { line: number; column: number }) => editor.gotoLine(pos.line, pos.column),
      { line, column },
    );
  }

  /** Returns "start", "end", or "" depending on whether the row has a fold widget. */
  async getFoldWidget(row: number): Promise<string> {
    return this.run((editor, r: number) => editor.session.getFoldWidget(r), row);
  }

  async getFoldWidgetRange(row: number): Promise<Ace.Range | null> {
    return this.run((editor, r: number) => {
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
    return this.run((editor, r: number) => editor.session.getLine(r), row);
  }

  async getTokens(row: number): Promise<AceToken[]> {
    return this.run(
      (editor, r: number) => editor.session.getTokens(r) as AceToken[],
      row,
    );
  }

  async getTokenAt(row: number, column: number): Promise<AceToken | null> {
    return this.run(
      (editor, pos: { row: number; column: number }) =>
        editor.session.getTokenAt(pos.row, pos.column) as AceToken | null,
      { row, column },
    );
  }

  /** Returns all Ace markers on the session, normalized to plain objects. */
  async getMarkers(): Promise<AceMarker[]> {
    return this.run((editor) => {
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
    return this.run((editor, r: number) => editor.session.getState(r), row);
  }

  /**
   * Returns the editor's multi-cursor selection ranges, normalized to plain
   * objects. Useful for verifying commands like renameInScope, which place a
   * cursor on every matching occurrence.
   */
  async getSelectionRanges(): Promise<Ace.Range[]> {
    return this.run((editor) => {
      const ranges = editor.selection.rangeList.ranges;
      return ranges.map((r) => ({
        start: { row: r.start.row, column: r.start.column },
        end: { row: r.end.row, column: r.end.column },
      }));
    });
  }

  async getCursorPosition(): Promise<Ace.Position> {
    return this.run((editor) => {
      const pos = editor.getCursorPosition();
      return { row: pos.row, column: pos.column };
    });
  }

  /** Equivalent to Ace's editor.find(needle): selects the first match and scrolls to it. */
  async find(needle: string): Promise<void> {
    await this.run((editor, n: string) => editor.find(n), needle);
  }

  /**
   * Insert text at the current cursor position (Ace's editor.insert).
   * Useful when typed-key delivery is hard to time (e.g. right after a save).
   */
  async insert(text: string): Promise<void> {
    await this.run((editor, t: string) => editor.insert(t), text);
  }

  /** Move cursor to the end of the current line (Ace's editor.navigateLineEnd). */
  async navigateLineEnd(): Promise<void> {
    await this.run((editor) => editor.navigateLineEnd());
  }

  /** Move focus to the editor textarea so subsequent page.keyboard input routes here. */
  async focus(): Promise<void> {
    await this.run((editor) => editor.focus());
  }

  /**
   * True when the renderer holds active ghost text (the mechanism behind
   * rstudioapi::setGhostText() and at-cursor completion previews). This is the
   * internal state Tab acts on, so it can stay set even after the ghost text
   * is no longer painted. Distinct from the assistant's synthetic ghost-text
   * tokens, which getTokens() reports.
   */
  async hasRendererGhostText(): Promise<boolean> {
    return this.run((editor) => {
      const renderer = (editor as unknown as { renderer?: { $ghostText?: unknown } }).renderer;
      return renderer?.$ghostText != null;
    });
  }
}
