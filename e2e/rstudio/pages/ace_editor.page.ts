import type { Page, JSHandle, Locator } from 'playwright';
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

/** A section header from the R code model's scope tree; see getSectionScopes. */
export interface AceSectionScope {
  label: string;
  row: number;
  depth?: number;
  parent: string | null;
}

/** A collapsed range and the folds nested inside it; see getFoldTree. */
export interface AceFoldTree {
  row: number;
  subFolds: AceFoldTree[];
}

export interface AceMarker {
  range: Ace.Range | null;
  type: string;
  clazz: string;
}

/**
 * Drives an Ace editor instance from Playwright via page.evaluate.
 *
 * Three ways to identify the target editor:
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
 *   - `AceEditor.visualModeChunk(page, marker, visualEditorRoot)`: an editor
 *     embedded in the specified visual editor, matched on content among only
 *     the chunk editors that Panmirror root has mounted. Neither of the other
 *     two reaches one: activeEditor() returns the (hidden) source editor for
 *     the document, and the plain marker walk would match that editor too,
 *     since in visual mode it still holds the whole document, chunk text
 *     included.
 *
 * The marker-substring path is a DOM walk and can land on stale editors left
 * in the DOM after a tab close (see ad175dccd1 / #17775 and #17784). Empty
 * marker avoids that entirely.
 */
export class AceEditor extends PageObject {
  private readonly marker: string;
  // Set only by visualModeChunk(); kept off the constructor so the public
  // signature stays free of a positional boolean.
  private inVisualEditor = false;
  private visualEditorRoot: Locator | null = null;

  constructor(page: Page, marker: string) {
    super(page);
    this.marker = marker;
  }

  /**
   * An Ace editor embedded in the visual editor -- a code chunk, or the YAML
   * front matter block -- identified by a substring of its contents. Matching
   * on content rather than position keeps a test independent of how many
   * editors panmirror mounts ahead of the chunk it means (the front matter
   * block is one of them).
   */
  static visualModeChunk(page: Page, marker: string, visualEditorRoot: Locator): AceEditor {
    const editor = new AceEditor(page, marker);
    editor.inVisualEditor = true;
    editor.visualEditorRoot = visualEditorRoot;
    return editor;
  }

  /**
   * Resolve the target editor in the browser and return a JSHandle to it.
   * Pairs with `run()` below, which disposes the handle when done.
   */
  private editorHandle(): Promise<JSHandle<Ace.Editor>> {
    if (this.inVisualEditor) {
      if (!this.visualEditorRoot) {
        throw new Error('AceEditor.visualModeChunk(): visual editor root is required');
      }

      return this.visualEditorRoot.evaluateHandle((root, marker): Ace.Editor => {
        const embedded = Array.from(root.querySelectorAll('.ace_editor'));
        for (let i = 0; i < embedded.length; i++) {
          const env = (embedded[i] as unknown as AceEditorElement).env;
          if (env?.editor && env.editor.getValue().indexOf(marker) !== -1) {
            return env.editor;
          }
        }
        throw new Error(
          `AceEditor.visualModeChunk('${marker}'): no embedded editor contains it `
          + `(${embedded.length} mounted in the specified visual editor)`,
        );
      }, this.marker);
    }

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

  /** Replace the editor's entire content (cursor lands at the end). */
  async setValue(content: string): Promise<void> {
    await this.run((editor, text: string) => editor.setValue(text, 1), content);
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

  /**
   * Returns the number of top-level collapsed ranges in the editor. Ace's
   * getAllFolds() walks the active fold lines only -- a fold nested inside a
   * collapsed parent lives on the parent's subFolds and is not counted -- so a
   * count of 0 means "nothing is folded", but a nonzero count is not the total
   * number of folds. Use getFoldTree() to see the nesting.
   */
  async getFoldCount(): Promise<number> {
    return this.run((editor) => editor.session.getAllFolds().length);
  }

  /**
   * Returns the collapsed ranges as a tree of document start rows, in document
   * order, with folds that Ace parked on a collapsed parent's subFolds nested
   * under that parent. Ace stores a subFold's range relative to its parent, so
   * the rows are translated back to absolute document rows here.
   */
  async getFoldTree(): Promise<AceFoldTree[]> {
    return this.run((editor) => {
      const visit = (folds: Ace.Fold[], base: number): AceFoldTree[] =>
        folds.map((fold) => {
          const row = base + fold.start.row;
          return { row, subFolds: visit(fold.subFolds, row) };
        });
      return visit(editor.session.getAllFolds(), 0);
    });
  }

  /**
   * Returns the section headers the R code model found, in document order --
   * the scopes that drive the document outline.
   *
   * `depth` is the header's heading level (1 for `#`, 2 for `##`, ...), or 0
   * for a header whose leading `#` run is decoration rather than a level -- a
   * bar such as `##########` or a banner such as `##### Section A #####`. It is
   * undefined for a section that has no notion of a heading level at all, such
   * as an R Markdown YAML block. `parent` is the label of the enclosing
   * section, or null when the section is top-level, so tests can assert how
   * headers nest. Only meaningful for modes with an R code model.
   */
  async getSectionScopes(): Promise<AceSectionScope[]> {
    return this.run((editor) => {
      const codeModel = editor.session.$mode?.codeModel;
      if (!codeModel?.getScopeTree) {
        throw new Error('getSectionScopes(): the editor mode has no R code model');
      }

      // getScopeTree() returns only the root's children, so walk the tree to
      // reach nested sections. Non-section scopes (functions, chunks) are
      // stepped through without becoming anyone's reported parent.
      const scopes: AceSectionScope[] = [];
      const visit = (nodes: Ace.Scope[], parent: string | null): void => {
        for (const node of nodes) {
          const isSection = node.isSection();
          if (isSection) {
            scopes.push({
              label: node.label,
              row: node.start.row,
              depth: node.attributes?.depth,
              parent,
            });
          }
          visit(node.$children ?? [], isSection ? node.label : parent);
        }
      };
      visit(codeModel.getScopeTree(), null);
      return scopes;
    });
  }

  /** Returns the raw text of `row` (0-indexed), excluding the trailing newline. */
  async getLine(row: number): Promise<string> {
    return this.run((editor, r: number) => editor.session.getLine(r), row);
  }

  /** Returns the last (partially) rendered row, 0-indexed. */
  async getLastVisibleRow(): Promise<number> {
    return this.run((editor) => editor.getLastVisibleRow());
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

  async getState(row: number): Promise<string | string[]> {
    return this.run((editor, r: number) => editor.session.getState(r), row);
  }

  /** Returns one level of indentation, e.g. "  " for two-space soft tabs. */
  async getTabString(): Promise<string> {
    return this.run((editor) => editor.session.getTabString());
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

  /** The current selection's text (Ace's editor.getSelectedText). */
  async getSelectedText(): Promise<string> {
    return this.run((editor) => editor.getSelectedText());
  }

  /** Rows as rendered (session.getScreenLength): exceeds the document line count exactly when soft-wrapped. */
  async getScreenRowCount(): Promise<number> {
    return this.run((editor) => editor.session.getScreenLength());
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

  /** True while the editor textarea has focus (Ace's editor.isFocused, absent from the typings). */
  async isFocused(): Promise<boolean> {
    return this.run((editor) => (editor as unknown as { isFocused(): boolean }).isFocused());
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

  /** Execute a built-in Ace editor command by name (e.g. 'addCursorBelow'). */
  async execCommand(name: string): Promise<void> {
    await this.run((editor, cmd: string) => editor.execCommand(cmd), name);
  }

  /**
   * Snapshot of the editor's multi-select bookkeeping, for asserting on
   * recovery from the corrupt states behind #13605. 'tempSelectionInstalled'
   * reports whether the throwaway Selection that forEachSelection swaps in
   * mid-iteration is still installed -- a sign the operation was aborted by
   * an exception and never restored its state.
   */
  async getMultiSelectState(): Promise<{
    editorInMultiSelectMode: boolean;
    selectionInMultiSelectMode: boolean;
    inVirtualSelectionMode: boolean;
    tempSelectionInstalled: boolean;
    rangeCount: number;
    rangeListAttached: boolean;
  }> {
    return this.run((editor) => {
      const session = editor.session;
      const selection = session.selection;
      return {
        editorInMultiSelectMode: !!editor.inMultiSelectMode,
        selectionInMultiSelectMode: !!selection.inMultiSelectMode,
        inVirtualSelectionMode: !!editor.inVirtualSelectionMode,
        tempSelectionInstalled:
          selection.index !== undefined ||
          (session.multiSelect != null && selection !== session.multiSelect),
        rangeCount: selection.rangeCount ?? 0,
        rangeListAttached: selection.rangeList != null && selection.rangeList.session != null,
      };
    });
  }

  /**
   * Fault injection for exception-safety tests: installs a one-shot document
   * 'change' listener that throws `message`, simulating a client exception
   * escaping into Ace's change dispatch mid-operation (#13605). The listener
   * removes itself and sets a window sentinel (see throwingChangeListenerFired)
   * before throwing, so only the next change is affected and tests can prove
   * the injection fired without depending on how the exception is routed.
   */
  async injectThrowingChangeListener(message: string): Promise<void> {
    await this.run((editor, msg: string) => {
      (window as { __throwingChangeListenerFired?: boolean }).__throwingChangeListenerFired = false;
      const doc = editor.session.getDocument();
      const listener = () => {
        doc.off('change', listener);
        (window as { __throwingChangeListenerFired?: boolean }).__throwingChangeListenerFired = true;
        throw new Error(msg);
      };
      doc.on('change', listener);
    }, message);
  }

  /** Whether the listener installed by injectThrowingChangeListener has thrown. */
  async throwingChangeListenerFired(): Promise<boolean> {
    return this.page.evaluate(
      () => !!(window as { __throwingChangeListenerFired?: boolean }).__throwingChangeListenerFired
    );
  }

  /**
   * Corrupt a live multi-select the way an aborted operation does: detach
   * the selection's range list so it stops tracking document edits (the
   * "range list detached" corruption branch of
   * AceEditorNative.getMultiSelectCorruptionReason).
   */
  async detachRangeList(): Promise<void> {
    await this.run((editor) => editor.session.selection.rangeList.detach());
  }
}
