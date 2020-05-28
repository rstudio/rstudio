/*
 * codemirror.ts
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

import { CodeViewOptions } from '../../api/node';
import { insertParagraph } from '../../api/paragraph';
import { createImageButton } from '../../api/widgets/widgets';
import { EditorUI } from '../../api/ui';
import { EditorOptions } from '../../api/options';
import { kPlatformMac } from '../../api/platform';
import { rmdChunk, previousExecutableRmdChunks, mergeRmdChunks } from '../../api/rmd';

import { selectAll } from '../../behaviors/select_all';

import CodeMirror from 'codemirror';

import 'codemirror/mode/clike/clike';
import 'codemirror/mode/javascript/javascript';
import 'codemirror/mode/xml/xml';
import 'codemirror/mode/css/css';
import 'codemirror/mode/markdown/markdown';
import 'codemirror/mode/python/python';
import 'codemirror/mode/r/r';
import 'codemirror/mode/shell/shell';
import 'codemirror/mode/sql/sql';
import 'codemirror/mode/stex/stex';
import 'codemirror/mode/yaml/yaml';
import 'codemirror/mode/yaml-frontmatter/yaml-frontmatter';

import 'codemirror/addon/selection/mark-selection.js';

import 'codemirror/lib/codemirror.css';
import './codemirror.css';

const plugin = new PluginKey('codemirror');

export function codeMirrorPlugins(
  codeViews: { [key: string]: CodeViewOptions },
  ui: EditorUI,
  options: EditorOptions,
): Plugin[] {
  // build nodeViews
  const nodeTypes = Object.keys(codeViews);
  const nodeViews: {
    [name: string]: (node: ProsemirrorNode<any>, view: EditorView<any>, getPos: boolean | (() => number)) => NodeView;
  } = {};
  nodeTypes.forEach(name => {
    nodeViews[name] = (node: ProsemirrorNode, view: EditorView, getPos: boolean | (() => number)) => {
      return new CodeBlockNodeView(node, view, getPos as () => number, ui, options, codeViews[name]);
    };
  });

  return [
    new Plugin({
      key: plugin,
      props: {
        nodeViews,
      },
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

// https://github.com/ProseMirror/website/blob/master/example/codemirror/index.js
class CodeBlockNodeView implements NodeView {
  public readonly dom: HTMLElement;
  private readonly view: EditorView;
  private readonly getPos: () => number;
  private readonly cm: CodeMirror.Editor;
  private readonly editorOptions: EditorOptions;
  private readonly options: CodeViewOptions;

  private readonly runChunkToolbar: HTMLDivElement;

  private node: ProsemirrorNode;
  private incomingChanges: boolean;
  private updating: boolean;

  constructor(
    node: ProsemirrorNode,
    view: EditorView,
    getPos: () => number,
    ui: EditorUI,
    editorOptions: EditorOptions,
    options: CodeViewOptions,
  ) {
    // Store for later
    this.node = node;
    this.view = view;
    this.getPos = getPos;
    this.incomingChanges = false;

    // options
    this.editorOptions = editorOptions;
    this.options = options;

    // CodeMirror options
    const cmOptions: any = {
      value: this.node.textContent,
      extraKeys: this.codeMirrorKeymap(),
      lineWrapping: true,
      indentUnit: 2,
      tabSize: 2,
      indentWithTabs: false,
      lineNumbers: this.options.lineNumbers,
      styleSelectedText: true,
    };
    if (this.options.lineNumberFormatter) {
      cmOptions.lineNumberFormatter = (lineNumber: number) => {
        const lineCount = this.cm?.lineCount();
        const line = this.cm?.getLine(lineNumber - 1);
        return this.options.lineNumberFormatter!(lineNumber, lineCount, line);
      };
    }

    // Create a CodeMirror instance
    this.cm = CodeMirror(null!, cmOptions as CodeMirror.EditorConfiguration);

    // The editor's outer node is our DOM representation
    this.dom = this.cm.getWrapperElement();

    // add a chunk execution button if execution is supported
    this.runChunkToolbar = this.initRunChunkToolbar(ui);
    this.dom.append(this.runChunkToolbar);

    // update mode
    this.updateMode();

    // theming
    this.dom.classList.add('pm-code-editor');
    this.dom.classList.add(options.borderColorClass || 'pm-block-border-color');
    if (this.options.classes) {
      this.options.classes.forEach(className => this.dom.classList.add(className));
    }
    if (options.firstLineMeta) {
      this.dom.classList.add('CodeMirror-firstLineMeta');
      this.cm.addLineClass(0, 'text', 'pm-light-text-color');
    }

    // CodeMirror needs to be in the DOM to properly initialize, so
    // schedule it to update itself
    setTimeout(() => {
      this.cm.refresh();

      // if we start out hidden then poll every 250ms to check if we
      // have been shown (after which time we need to call refresh())
      if (this.dom.offsetHeight === 0) {
        const timerId = setInterval(() => {
          if (this.dom.offsetHeight > 0) {
            this.cm.refresh();
            clearInterval(timerId);
          }
        }, 250);
      }
    }, 20);

    // This flag is used to avoid an update loop between the outer and
    // inner editor
    this.updating = false;
    // Track whether changes are have been made but not yet propagated
    this.cm.on('beforeChange', () => (this.incomingChanges = true));
    // Propagate updates from the code editor to ProseMirror
    this.cm.on('cursorActivity', () => {
      if (!this.updating && !this.incomingChanges) {
        this.forwardSelection();
      }
    });
    this.cm.on('changes', () => {
      if (!this.updating) {
        this.valueChanged();
        this.forwardSelection();
      }
      this.incomingChanges = false;
    });

    this.cm.on('focus', () => this.forwardSelection());
  }

  public update(node: ProsemirrorNode, _decos: Decoration[]) {
    if (node.type !== this.node.type) {
      return false;
    }
    this.node = node;
    this.updateMode();
    const change = computeChange(this.cm.getValue(), node.textContent);
    if (change) {
      this.updating = true;
      const cmDoc = this.cm.getDoc();
      cmDoc.replaceRange(change.text, cmDoc.posFromIndex(change.from), cmDoc.posFromIndex(change.to));
      this.updating = false;
    }
    return true;
  }

  public setSelection(anchor: number, head: number) {
    this.cm.focus();
    this.cm.getWrapperElement().classList.add('CodeMirror-focused');
    this.updating = true;
    const cmDoc = this.cm.getDoc();
    cmDoc.setSelection(cmDoc.posFromIndex(anchor), cmDoc.posFromIndex(head));
    this.updating = false;
  }

  public selectNode() {
    this.cm.focus();
  }

  public stopEvent() {
    return true;
  }

  private forwardSelection() {
    if (!this.cm.hasFocus()) {
      return;
    }

    const state = this.view.state;
    const selection = this.asProseMirrorSelection(state.doc);
    if (!selection.eq(state.selection)) {
      this.view.dispatch(state.tr.setSelection(selection));
    }
  }

  private asProseMirrorSelection(doc: ProsemirrorNode) {
    const offset = this.getPos() + 1;
    const cmDoc = this.cm.getDoc();
    const anchor = cmDoc.indexFromPos(cmDoc.getCursor('anchor')) + offset;
    const head = cmDoc.indexFromPos(cmDoc.getCursor('head')) + offset;
    return TextSelection.create(doc, anchor, head);
  }

  private valueChanged() {
    const change = computeChange(this.node.textContent, this.cm.getValue());
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

  private updateMode() {
    // get lang
    const lang = this.options.lang(this.node, this.cm.getValue());

    // syntax highlighting
    const mode = lang ? modeForLang(lang, this.options) : null;
    if (mode !== this.cm.getOption('mode')) {
      this.cm.setOption('mode', mode);
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

  private codeMirrorKeymap() {
    const view = this.view;
    const mod = kPlatformMac ? 'Cmd' : 'Ctrl';

    // exit code block
    const exitBlock = () => {
      if (exitCode(view.state, view.dispatch)) {
        view.focus();
      }
    };

    // Note: normalizeKeyMap not declared in CodeMirror types
    // so we cast to any
    const cmKeymap = (CodeMirror as any).normalizeKeyMap({
      Up: () => this.arrowMaybeEscape('line', -1),
      Left: () => this.arrowMaybeEscape('char', -1),
      Down: () => this.arrowMaybeEscape('line', 1),
      Right: () => this.arrowMaybeEscape('char', 1),
      Backspace: () => this.backspaceMaybeDeleteNode(),
      Tab: () => this.cm.execCommand(this.cm.getOption('indentWithTabs') ? 'insertTab' : 'insertSoftTab'),
      // undo/redo keys are technically rebindable in the parent
      // editor so we may need a way to propagate the rebinding here
      [`${mod}-Z`]: () => undo(view.state, view.dispatch),
      [`Shift-${mod}-Z`]: () => redo(view.state, view.dispatch),
      [`${mod}-Y`]: () => redo(view.state, view.dispatch),
      [`${mod}-A`]: () => selectAll(view.state, view.dispatch, view),
      'Ctrl-Enter': exitBlock,
      'Shift-Enter': exitBlock,
      [`${mod}-Enter`]: exitBlock,
      [`${mod}-\\`]: () => insertParagraph(view.state, view.dispatch),
      F4: () => {
        return this.options.attrEditFn ? this.options.attrEditFn(view.state, view.dispatch, view) : CodeMirror.Pass;
      },
    });
    return cmKeymap;
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
    } else {
      return CodeMirror.Pass;
    }
  }

  private arrowMaybeEscape(unit: string, dir: number) {
    const cmDoc = this.cm.getDoc();
    const pos = cmDoc.getCursor();
    if (
      cmDoc.somethingSelected() ||
      pos.line !== (dir < 0 ? cmDoc.firstLine() : cmDoc.lastLine()) ||
      (unit === 'char' && pos.ch !== (dir < 0 ? 0 : cmDoc.getLine(pos.line).length))
    ) {
      return CodeMirror.Pass;
    }

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
  }

  private initRunChunkToolbar(ui: EditorUI) {
    const toolbar = window.document.createElement('div');
    toolbar.classList.add('pm-codemirror-toolbar');
    if (this.options.executeRmdChunkFn) {
      // run previous chunks button
      const runPreivousChunkShortcut = kPlatformMac ? '⌥⌘P' : 'Ctrl+Alt+P';
      const runPreviousChunksButton = createImageButton(
        ui.images.runprevchunks!,
        ['pm-run-previous-chunks-button'],
        `${ui.context.translateText('Run All Chunks Above')} (${runPreivousChunkShortcut})`,
      );
      runPreviousChunksButton.onclick = this.executePreviousChunks.bind(this);
      toolbar.append(runPreviousChunksButton);

      // run chunk button
      const runChunkShortcut = kPlatformMac ? '⇧⌘↩︎' : 'Ctrl+Shift+Enter';
      const runChunkButton = createImageButton(
        ui.images.runchunk!,
        ['pm-run-chunk-button'],
        `${ui.context.translateText('Run Chunk')} (${runChunkShortcut})`,
      );
      runChunkButton.onclick = this.executeChunk.bind(this);
      toolbar.append(runChunkButton);
    }

    return toolbar;
  }

  private executeChunk() {
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

const kModeMap: { [key: string]: string } = {
  r: 'r',
  python: 'python',
  sql: 'sql',
  c: 'clike',
  cpp: 'clike',
  java: 'clike',
  js: 'javascript',
  javascript: 'javascript',
  html: 'xml',
  tex: 'stex',
  latex: 'stex',
  css: 'css',
  markdown: 'markdown',
  yaml: 'yaml',
  'yaml-frontmatter': 'yaml-frontmatter',
  shell: 'shell',
  bash: 'bash',
};

const kBookdownThereomModeMap: { [key: string]: string } = {
  theorem: 'stex',
  lemma: 'stex',
  corollary: 'stex',
  proposition: 'stex',
  conjecture: 'stex',
  definition: 'stex',
  example: 'stex',
  exercise: 'stex',
};

function modeForLang(lang: string, options: CodeViewOptions) {
  const modeMap = {
    ...kModeMap,
    ...(options.bookdownTheorems ? kBookdownThereomModeMap : {}),
  };

  if (modeMap.hasOwnProperty(lang)) {
    return modeMap[lang];
  } else {
    return null;
  }
}
