/*
 * ace.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import {
  Plugin,
  PluginKey,
  TextSelection,
  EditorState,
  Transaction,
  Selection,
  NodeSelection,
} from 'prosemirror-state';

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorView, NodeView, Decoration } from 'prosemirror-view';
import { undo, redo } from 'prosemirror-history';
import { exitCode } from 'prosemirror-commands';
import { keymap } from 'prosemirror-keymap';
import { undoInputRule } from 'prosemirror-inputrules';

import { CodeViewOptions, editingRootNode } from '../../api/node';
import { insertParagraph } from '../../api/paragraph';
import { createImageButton } from '../../api/widgets/widgets';
import { EditorUI, ChunkEditor } from '../../api/ui';
import { EditorOptions } from '../../api/options';
import { kPlatformMac } from '../../api/platform';
import { rmdChunk, previousExecutableRmdChunks, mergeRmdChunks } from '../../api/rmd';
import { ExtensionContext } from '../../api/extension';
import { DispatchEvent } from '../../api/event-types';

import { selectAll } from '../../behaviors/select_all';
import { findPluginState } from '../../behaviors/find';

import './ace.css';
import { AceRenderQueue } from './render_queue';
import { AcePlaceholder } from './ace_placeholder';

const plugin = new PluginKey('ace');

export function acePlugins(
  codeViews: { [key: string]: CodeViewOptions },
  context: ExtensionContext
): Plugin[] {
  // build nodeViews
  const nodeTypes = Object.keys(codeViews);
  const renderQueue = new AceRenderQueue();
  const nodeViews: {
    [name: string]: (node: ProsemirrorNode<any>, view: EditorView<any>, getPos: boolean | (() => number)) => NodeView;
  } = {};
  nodeTypes.forEach(name => {
    nodeViews[name] = (node: ProsemirrorNode, view: EditorView, getPos: boolean | (() => number)) => {
      return new CodeBlockNodeView(node, view, getPos as () => number, context, codeViews[name], renderQueue);
    };
  });

  return [
    new Plugin({
      key: plugin,
      props: {
        nodeViews,
      }
    }),
    // arrow in and out of editor
    keymap({
      ArrowLeft: arrowHandler('left', nodeTypes),
      ArrowRight: arrowHandler('right', nodeTypes),
      ArrowUp: arrowHandler('up', nodeTypes),
      ArrowDown: arrowHandler('down', nodeTypes),
    }),
  ];
}

export class CodeBlockNodeView implements NodeView {
  public readonly dom: HTMLElement;
  private readonly view: EditorView;
  private readonly getPos: () => number;
  private readonly ui: EditorUI;
  private readonly renderQueue: AceRenderQueue;
  private chunk?: ChunkEditor;
  private aceEditor?: AceAjax.Editor;
  private editSession?: AceAjax.IEditSession;
  private readonly editorOptions: EditorOptions;
  private readonly options: CodeViewOptions;

  private readonly runChunkToolbar: HTMLDivElement;

  private node: ProsemirrorNode;
  private updating: boolean;
  private escaping: boolean;
  private mode: string;
  private findMarkers: number[];
  private selectionMarker: number | null;

  private dispatchUnsubscribe: VoidFunction;

  constructor(
    node: ProsemirrorNode,
    view: EditorView,
    getPos: () => number,
    context: ExtensionContext,
    options: CodeViewOptions,
    renderQueue: AceRenderQueue
  ) {
    // context
    const ui = context.ui;
    const events = context.events;
    const editorOptions = context.options;

    // Store for later
    this.node = node;
    this.view = view;
    this.ui = context.ui;
    this.getPos = getPos;
    this.mode = "";
    this.escaping = false;
    this.findMarkers = [];
    this.selectionMarker = null;
    this.renderQueue = renderQueue;

    // options
    this.editorOptions = editorOptions;
    this.options = options;

    // The editor's outer node is our DOM representation
    this.dom = document.createElement("div");
    this.dom.classList.add('pm-code-editor');
    this.dom.classList.add('pm-ace-editor');
    this.dom.classList.add('pm-ace-editor-inactive');
    this.dom.classList.add(options.borderColorClass || 'pm-block-border-color');
    if (this.options.classes) {
      this.options.classes.forEach(className => this.dom.classList.add(className));
    }

    // Create a preview of the text (will be shown until editor is fully initialized)
    const preview = new AcePlaceholder(node.textContent);
    this.dom.appendChild(preview.getElement());

    // Style the first line differently if requested
    if (options.firstLineMeta) {
      this.dom.classList.add('pm-ace-first-line-meta');
    }

    // add a chunk execution button if execution is supported
    this.runChunkToolbar = this.initRunChunkToolbar(ui);
    this.dom.append(this.runChunkToolbar);

    // update mode
    this.updateMode();

    // observe all editor dispatches
    this.dispatchUnsubscribe = events.subscribe(DispatchEvent, (tr: Transaction | undefined) => {
      if (tr) {
        this.onEditorDispatch(tr);
      }
    });

    // This flag is used to avoid an update loop between the outer and
    // inner editor
    this.updating = false;

    if (renderQueue.isRenderCompleted()) {
      // All editors have been rendered and the queue is empty; initialize
      // directly (this happens when e.g., inserting code chunks interactively
      // after the document is fully rendered)
      this.initEditor();
    } else {
      // Rendering is not complete; add to the queue
      renderQueue.add(this);
    }
  }

  public destroy() {
    // Unsubscribe from events
    this.dispatchUnsubscribe();

    // Clean up attached editor instance when it's removed from the DOM
    if (this.chunk) {
      this.chunk.destroy();
    }
  }

  public update(node: ProsemirrorNode, _decos: Decoration[]) {
    if (node.type !== this.node.type) {
      return false;
    }
    if (!this.editSession) {
      return false;
    }
    this.node = node;
    this.updateMode();

    const AceRange = window.require("ace/range").Range;
    const doc = this.editSession.getDocument();

    const change = computeChange(this.editSession.getValue(), node.textContent);
    if (change) {
      this.updating = true;
      const range = AceRange.fromPoints(doc.indexToPosition(change.from, 0),
        doc.indexToPosition(change.to, 0));
      this.editSession.replace(range, change.text);
      this.updating = false;
    }

    // Clear any previously rendered find markers
    this.findMarkers.forEach(id => {
      if (this.editSession) {
        this.editSession.removeMarker(id);
      }
    });
    this.findMarkers = [];

    // Get all of the find results inside this node
    const decorations = findPluginState(this.view.state);
    if (decorations) {
      const decos = decorations?.find(this.getPos(), (this.getPos() + node.nodeSize) - 1);

      // If we got results, render them
      if (decos) {
        decos.forEach((deco: any) => {
          if (!this.editSession) {
            return;
          }

          // Calculate from/to position for result marker (adjust for zero based column)
          const markerFrom = doc.indexToPosition(deco.from - this.getPos(), 0);
          markerFrom.column--;
          const markerTo = doc.indexToPosition(deco.to - this.getPos(), 0);
          markerTo.column--;
          const range = AceRange.fromPoints(markerFrom, markerTo);

          // Create the search result marker and add it to the rendered set
          const markerId = this.editSession.addMarker(range, deco.type.attrs.class, "result", true);
          this.findMarkers.push(markerId);
        });
      }
    }

    return true;
  }

  public setSelection(anchor: number, head: number) {
    if (!this.aceEditor || !this.editSession) {
      return;
    }
    if (!this.escaping) {
      this.aceEditor.focus();
    }
    this.updating = true;
    const doc = this.editSession.getDocument();
    const AceRange = window.require("ace/range").Range;
    const range = AceRange.fromPoints(doc.indexToPosition(anchor, 0),
      doc.indexToPosition(head, 0));
    this.editSession.getSelection().setSelectionRange(range);
    this.updating = false;
  }

  public selectNode() {
    if (this.aceEditor) {
      this.aceEditor.focus();
    }
  }

  public stopEvent() {
    return true;
  }

  private onEditorDispatch(tr: Transaction) {
    if (tr.selectionSet) {
      this.highlightSelectionAcross(tr.selection);
    }
  }

  private forwardSelection() {
    // ignore if we don't have focus
    if (!this.chunk ||
      !this.chunk.element.contains(window.document.activeElement)) {
      return;
    }

    const state = this.view.state;
    const selection = this.asProseMirrorSelection(state.doc);
    if (selection && !selection.eq(state.selection)) {
      this.view.dispatch(state.tr.setSelection(selection));
    }
  }

  private asProseMirrorSelection(doc: ProsemirrorNode) {
    if (!this.editSession) {
      return null;
    }
    const offset = this.getPos() + 1;
    const session = this.editSession;
    const range = session.getSelection().getRange();
    const anchor = session.getDocument().positionToIndex(range.start, 0) + offset;
    const head = session.getDocument().positionToIndex(range.end, 0) + offset;
    return TextSelection.create(doc, anchor, head);
  }

  // detect the entire editor being selected across, in which case we add an ace marker 
  // visually indicating that the text is selected
  private highlightSelectionAcross(selection: Selection) {
    if (!this.aceEditor || !this.editSession) {
      return;
    }

    // clear any existing selection marker
    if (this.selectionMarker !== null) {
      this.editSession.removeMarker(this.selectionMarker);
      this.selectionMarker = null;
    }

    // check for selection spanning us
    const pos = this.getPos();
    if ((selection.from < pos) && (selection.to > pos + this.node.nodeSize)) {
      const doc = this.editSession.getDocument();
      const AceRange = window.require("ace/range").Range;
      const range = AceRange.fromPoints(doc.indexToPosition(0, 0), doc.indexToPosition(this.node.nodeSize - 1, 0));
      this.selectionMarker = this.editSession.addMarker(range, 'pm-selected-text', "selection", true);
    }
  }

  private valueChanged() {
    const change = computeChange(this.node.textContent, this.getContents());
    if (change) {
      // update content
      const start = this.getPos() + 1;
      const tr = this.view.state.tr.replaceWith(
        start + change.from,
        start + change.to,
        change.text ? this.node.type.schema.text(change.text) : null,
      );
      this.view.dispatch(tr);
    }
    this.updateMode();
  }

  /**
   * Initializes the editing surface by creating and injecting an Ace editor
   * instance from the host.
   */
  public initEditor() {
    // skip if we're already initialized
    if (this.aceEditor) {
      return;
    }

    // call host factory to instantiate editor and populate with initial content
    this.chunk = this.ui.chunks.createChunkEditor('ace');
    this.aceEditor = this.chunk.editor as AceAjax.Editor;
    this.aceEditor.setValue(this.node.textContent);
    this.aceEditor.clearSelection();

    // cache edit session for convenience; most operations happen on the session
    this.editSession = this.aceEditor.getSession();

    // remove the preview and recreate chunk toolbar
    this.dom.innerHTML = "";
    this.dom.appendChild(this.chunk.element);
    this.dom.append(this.runChunkToolbar);

    // Propagate updates from the code editor to ProseMirror
    this.aceEditor.on("changeCursor", () => {
      if (!this.updating) {
        this.forwardSelection();
      }
    });
    this.aceEditor.on('change', () => {
      if (!this.updating) {
        this.valueChanged();
        this.forwardSelection();
      }
    });

    // Forward selection we we receive it
    this.aceEditor.on('focus', () => {
      this.dom.classList.remove("pm-ace-editor-inactive");
      this.forwardSelection();
    });

    this.aceEditor.on('blur', () => {
      // Add a class to editor; this class contains CSS rules that hide editor
      // components that Ace cannot hide natively (such as the cursor and
      // matching bracket indicator)
      this.dom.classList.add("pm-ace-editor-inactive");

      // Clear the selection (otherwise could conflict with Prosemirror's
      // selection)
      if (this.editSession) {
        this.editSession.selection.clearSelection();
      }
    });

    // Add custom escape commands for movement keys (left/right/up/down); these
    // will check to see whether the movement should leave the editor, and if
    // so will do so instead of moving the cursor.
    this.aceEditor.commands.addCommand({
      name: "leftEscape",
      bindKey: "Left",
      exec: () => { this.arrowMaybeEscape('char', -1, "gotoleft"); }
    });
    this.aceEditor.commands.addCommand({
      name: "rightEscape",
      bindKey: "Right",
      exec: () => { this.arrowMaybeEscape('char', 1, "gotoright"); }
    });
    this.aceEditor.commands.addCommand({
      name: "upEscape",
      bindKey: "Up",
      exec: () => { this.arrowMaybeEscape('line', -1, "golineup"); }
    });
    this.aceEditor.commands.addCommand({
      name: "downEscape",
      bindKey: "Down",
      exec: () => { this.arrowMaybeEscape('line', 1, "golinedown"); }
    });

    // Pressing Backspace in the editor when it's empty should delete it.
    this.aceEditor.commands.addCommand({
      name: "backspaceDeleteNode",
      bindKey: "Backspace",
      exec: () => { this.backspaceMaybeDeleteNode(); }
    });

    // Handle undo/redo in ProseMirror
    this.aceEditor.commands.addCommand({
      name: "undoProsemirror",
      bindKey: {
        win: "Ctrl-Z",
        mac: "Command-Z"
      },
      exec: () => {
        if (undo(this.view.state, this.view.dispatch)) {
          this.view.focus();
        }
      }
    });
    this.aceEditor.commands.addCommand({
      name: "redoProsemirror",
      bindKey: {
        win: "Ctrl-Shift-Z|Ctrl-Y",
        mac: "Command-Shift-Z|Command-Y"
      },
      exec: () => {
        if (redo(this.view.state, this.view.dispatch)) {
          this.view.focus();
        }

      }
    });

    // Handle Select All in ProseMirror
    this.aceEditor.commands.addCommand({
      name: "selectAllProsemirror",
      bindKey: {
        win: "Ctrl-A",
        mac: "Command-A"
      },
      exec: () => {
        if (selectAll(this.view.state, this.view.dispatch, this.view)) {
          this.view.focus();
        }
      }
    });

    // Handle shortcuts for moving focus out of the code editor and into
    // ProseMirror
    this.aceEditor.commands.addCommand({
      name: "exitCodeBlock",
      bindKey: {
        win: "Ctrl-Enter|Shift-Enter",
        mac: "Ctrl-Enter|Shift-Enter|Command-Enter"
      },
      exec: () => {
        if (exitCode(this.view.state, this.view.dispatch)) {
          this.view.focus();
        }
      }
    });

    // Create a command for inserting paragraphs from the code editor
    this.aceEditor.commands.addCommand({
      name: "insertParagraph",
      bindKey: {
        win: "Ctrl-\\",
        mac: "Command-\\"
      },
      exec: () => {
        if (insertParagraph(this.view.state, this.view.dispatch)) {
          this.view.focus();
        }
      }
    });

    // If an attribute editor function was supplied, bind it to F4
    if (this.options.attrEditFn) {
      this.aceEditor.commands.addCommand({
        name: "editAttributes",
        bindKey: "F4",
        exec: () => { this.options.attrEditFn!(this.view.state, this.view.dispatch, this.view); }
      });
    }

    // Apply editor mode
    if (this.mode) {
      this.chunk.setMode(this.mode);
    }

    // Disconnect font metrics system after render loop
    (this.aceEditor.renderer as any).on("afterRender", () => {
      window.setTimeout(() => {
        if (this.aceEditor) {
          const metrics = (this.aceEditor.renderer as any).$fontMetrics;
          if (metrics && metrics.$observer) {
            metrics.$observer.disconnect();
          }
        }
      }, 0);
    });

    // Hook up the container to the render queue
    const editingRoot = editingRootNode(this.view.state.selection)!;
    const container = this.view.nodeDOM(editingRoot.pos) as HTMLElement;
    if (container.parentElement) {
      this.renderQueue.setContainer(container);
    }
  }

  private updateMode() {
    // get lang
    const lang = this.options.lang(this.node, this.getContents());

    if (lang !== null && this.mode !== lang) {
      if (this.chunk) {
        this.chunk.setMode(lang);
      }
      this.mode = lang;
    }

    // if we have a language check whether execution should be enabled for this language
    if (lang && this.canExecuteChunks()) {
      const enabled = !!this.editorOptions.rmdChunkExecution!.find(rmdChunkLang => {
        return lang.localeCompare(rmdChunkLang, undefined, { sensitivity: 'accent' }) === 0;
      });
      this.enableChunkExecution(enabled);
    } else {
      this.enableChunkExecution(false);
    }
  }

  private backspaceMaybeDeleteNode() {
    // if the node is empty and we execute a backspace then delete the node
    if (this.node.childCount === 0) {
      // if there is an input rule we just executed then use this to undo it
      if (undoInputRule(this.view.state)) {
        undoInputRule(this.view.state, this.view.dispatch);
        this.view.focus();
      } else {
        const tr = this.view.state.tr;
        tr.delete(this.getPos(), this.getPos() + this.node.nodeSize);
        tr.setSelection(TextSelection.near(tr.doc.resolve(this.getPos()), -1));
        this.view.dispatch(tr);
        this.view.focus();
      }
    } else if (this.aceEditor) {
      this.aceEditor.execCommand("backspace");
    }
  }


  // Checks to see whether an arrow key should escape the editor or not. If so,
  // sends the focus to the right node; if not, executes the given Ace command
  // (to perform the arrow key's usual action)
  private arrowMaybeEscape(unit: string, dir: number, command: string) {
    if (!this.aceEditor || !this.editSession) {
      return;
    }
    const pos = this.aceEditor.getCursorPosition();
    const lastrow = this.editSession.getLength() - 1;
    if ((!this.aceEditor.getSelection().isEmpty()) ||
      pos.row !== (dir < 0 ? 0 : lastrow) ||
      (unit === 'char' && pos.column !== (dir < 0 ? 0 : this.editSession.getDocument().getLine(pos.row).length))) {
      // this movement is happening inside the editor itself. don't escape
      // the editor; just execute the underlying command
      this.aceEditor.execCommand(command);
      return;
    }

    // the cursor is about to leave the editor region; flag this to avoid side
    // effects
    this.escaping = true;

    // ensure we are focused
    this.view.focus();

    // get the current position
    const $pos = this.view.state.doc.resolve(this.getPos());

    // helpers to figure out if the previous or next nodes are selectable
    const prevNodeSelectable = () => {
      return $pos.nodeBefore && $pos.nodeBefore.type.spec.selectable;
    };
    const nextNodeSelectable = () => {
      const nextNode = this.view.state.doc.nodeAt(this.getPos() + this.node.nodeSize);
      return nextNode?.type.spec.selectable;
    };

    // see if we can get a new selection
    const tr = this.view.state.tr;
    let selection: Selection | undefined;

    // if we are going backwards and the previous node can take node selections then select it
    if (dir < 0 && prevNodeSelectable()) {
      const prevNodePos = this.getPos() - $pos.nodeBefore!.nodeSize;
      selection = NodeSelection.create(tr.doc, prevNodePos);

      // if we are going forwards and the next node can take node selections then select it
    } else if (dir >= 0 && nextNodeSelectable()) {
      const nextNodePos = this.getPos() + this.node.nodeSize;
      selection = NodeSelection.create(tr.doc, nextNodePos);

      // otherwise use text selection handling (handles forward/backward text selections)
    } else {
      const targetPos = this.getPos() + (dir < 0 ? 0 : this.node.nodeSize);
      const targetNode = this.view.state.doc.nodeAt(targetPos);
      if (targetNode) {
        selection = Selection.near(this.view.state.doc.resolve(targetPos), dir);
      }
    }

    // set selection if we've got it
    if (selection) {
      tr.setSelection(selection).scrollIntoView();
      this.view.dispatch(tr);
    }

    // set focus
    this.view.focus();
    this.escaping = false;
  }

  private initRunChunkToolbar(ui: EditorUI) {
    const toolbar = window.document.createElement('div');
    toolbar.classList.add('pm-ace-toolbar');
    if (this.options.executeRmdChunkFn) {
      // run previous chunks button
      const runPreivousChunkShortcut = kPlatformMac ? '⌥⌘P' : 'Ctrl+Alt+P';
      const runPreviousChunksButton = createImageButton(
        ui.images.runprevchunks!,
        ['pm-run-previous-chunks-button'],
        `${ui.context.translateText('Run All Chunks Above')} (${runPreivousChunkShortcut})`,
      );
      runPreviousChunksButton.tabIndex = -1;
      runPreviousChunksButton.onclick = this.executePreviousChunks.bind(this);
      toolbar.append(runPreviousChunksButton);

      // run chunk button
      const runChunkShortcut = kPlatformMac ? '⇧⌘↩︎' : 'Ctrl+Shift+Enter';
      const runChunkButton = createImageButton(
        ui.images.runchunk!,
        ['pm-run-chunk-button'],
        `${ui.context.translateText('Run Chunk')} (${runChunkShortcut})`,
      );
      runChunkButton.tabIndex = -1;
      runChunkButton.onclick = this.executeChunk.bind(this);
      toolbar.append(runChunkButton);
    }

    return toolbar;
  }

  private executeChunk() {
    // ensure editor is rendered
    if (!this.aceEditor) {
      this.initEditor();
    }
    if (this.isChunkExecutionEnabled()) {
      const chunk = rmdChunk(this.node.textContent);
      if (chunk != null) {
        this.options.executeRmdChunkFn!(chunk);
      }
    }
  }

  private executePreviousChunks() {
    if (this.isChunkExecutionEnabled()) {
      const prevChunks = previousExecutableRmdChunks(this.view.state, this.getPos());
      const mergedChunk = mergeRmdChunks(prevChunks);
      if (mergedChunk) {
        this.options.executeRmdChunkFn!(mergedChunk);
      }
    }
  }

  private canExecuteChunks() {
    return this.editorOptions.rmdChunkExecution && this.options.executeRmdChunkFn;
  }

  private enableChunkExecution(enable: boolean) {
    this.runChunkToolbar.style.display = enable ? 'initial' : 'none';
  }

  private isChunkExecutionEnabled() {
    return this.runChunkToolbar.style.display !== 'none';
  }

  private getContents(): string {
    if (this.editSession) {
      return this.editSession.getValue();
    } else {
      return this.dom.innerText;
    }
  }
}

function computeChange(oldVal: string, newVal: string) {
  if (oldVal === newVal) {
    return null;
  }
  let start = 0;
  let oldEnd = oldVal.length;
  let newEnd = newVal.length;
  while (start < oldEnd && oldVal.charCodeAt(start) === newVal.charCodeAt(start)) {
    ++start;
  }
  while (oldEnd > start && newEnd > start && oldVal.charCodeAt(oldEnd - 1) === newVal.charCodeAt(newEnd - 1)) {
    oldEnd--;
    newEnd--;
  }
  return {
    from: start,
    to: oldEnd,
    text: newVal.slice(start, newEnd),
  };
}

function arrowHandler(dir: 'up' | 'down' | 'left' | 'right', nodeTypes: string[]) {
  return (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
    if (state.selection.empty && view && view.endOfTextblock(dir)) {
      const side = dir === 'left' || dir === 'up' ? -1 : 1;
      const $head = state.selection.$head;
      const nextPos = Selection.near(state.doc.resolve(side > 0 ? $head.after() : $head.before()), side);
      if (nextPos.$head && nodeTypes.includes(nextPos.$head.parent.type.name)) {
        if (dispatch) {
          dispatch(state.tr.setSelection(nextPos));
        }
        return true;
      }
    }
    return false;
  };
}

