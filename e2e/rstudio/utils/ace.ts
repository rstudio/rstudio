// TypeScript bindings for the runtime Ace editor API as exposed by RStudio.
// These describe just the methods and fields the Playwright tests currently
// reach into via page.evaluate -- add new entries here when a test needs
// them instead of inlining cast-and-assert blocks at the call site.
//
// The namespace mirrors Ace's own naming (Ace.Editor, Ace.Range, etc.) so
// the bindings are discoverable and don't collide with the AceEditor
// page-object class in pages/ace_editor.page.ts (which is the Playwright
// wrapper, not the runtime instance).

export namespace Ace {
  export interface Position {
    row: number;
    column: number;
  }

  export interface Range {
    start: Position;
    end: Position;
  }

  export interface Selection {
    setRange(range: Range): void;
    rangeList: { ranges: Range[]; session?: unknown | null };
    // Multi-select bookkeeping (see Ace's multi_select.js). 'index' is set
    // only on the temporary Selection that forEachSelection installs while
    // iterating, so its presence on session.selection indicates an aborted
    // multi-select operation (#13605).
    inMultiSelectMode?: boolean;
    ranges?: Range[] | null;
    rangeCount?: number;
    index?: number;
  }

  export interface Document {
    on(event: string, fn: (e: unknown) => void): void;
    off(event: string, fn: (e: unknown) => void): void;
  }

  // Methods on the EditSession (editor.session). Ace exposes many more --
  // restrict to the ones tests actually need to keep the surface obvious.
  export interface Session {
    getLength(): number;
    getLine(row: number): string;
    getFoldWidget(row: number): string;
    getFoldWidgetRange(row: number): Range | null;
    getState(row: number): string;
    getTokens(row: number): unknown[];
    getTokenAt(row: number, column: number): unknown | null;
    getMarkers(): Record<string, unknown>;
    replace(range: Range, text: string): Position;
    remove(range: Range): Position;
    getDocument(): Document;
    selection: Selection;
    // The session's real Selection object; session.selection may briefly
    // point elsewhere during multi-select operations (see Ace's
    // multi_select.js onSessionChange).
    multiSelect?: Selection;
  }

  // The runtime editor instance. Hung off the .ace_editor DOM element via
  // the .env.editor backref (see AceEditorElement below).
  export interface Editor {
    session: Session;
    selection: Selection;
    getValue(): string;
    setValue(value: string, cursorPos?: number): void;
    focus(): void;
    gotoLine(line: number, column?: number): void;
    getCursorPosition(): Position;
    getSelectedText(): string;
    getFirstVisibleRow(): number;
    find(needle: string): void;
    insert(text: string): void;
    navigateLineEnd(): void;
    selectAll(): void;
    execCommand(name: string): void;
    exitMultiSelectMode(): void;
    inMultiSelectMode?: boolean;
    inVirtualSelectionMode?: boolean;
  }
}

// Shape of a DOM element with the Ace runtime instance attached. Use with
// document.getElementById when the editor is identified by a stable id
// (e.g. #rstudio_console_input). For source editors that don't have a
// stable id, iterate document.querySelectorAll('.ace_editor') and cast
// elements with this type.
export interface AceEditorElement extends HTMLElement {
  env?: { editor?: Ace.Editor };
}
