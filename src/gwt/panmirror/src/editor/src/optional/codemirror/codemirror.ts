/*
 * codemirror.ts
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import { Plugin, PluginKey, TextSelection, EditorState, Transaction, Selection } from 'prosemirror-state';
import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorView, NodeView } from 'prosemirror-view';
import { undo, redo } from 'prosemirror-history';
import { exitCode } from 'prosemirror-commands';
import { keymap } from 'prosemirror-keymap';
import { undoInputRule } from 'prosemirror-inputrules';

import { CodeViewOptions } from '../../api/node';

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

export function codeMirrorPlugins(codeViews: { [key: string]: CodeViewOptions }) {
  // build nodeViews
  const nodeTypes = Object.keys(codeViews);
  const nodeViews: {
    [name: string]: (node: ProsemirrorNode<any>, view: EditorView<any>, getPos: boolean | (() => number)) => NodeView;
  } = {};
  nodeTypes.forEach(name => {
    nodeViews[name] = (node: ProsemirrorNode, view: EditorView, getPos: boolean | (() => number)) => {
      return new CodeBlockNodeView(node, view, getPos as () => number, codeViews[name]);
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
  private readonly options: CodeViewOptions;

  private node: ProsemirrorNode;
  private incomingChanges: boolean;
  private updating: boolean;

  constructor(node: ProsemirrorNode, view: EditorView, getPos: () => number, options: CodeViewOptions) {
    // Store for later
    this.node = node;
    this.view = view;
    this.getPos = getPos;
    this.incomingChanges = false;

    // option defaults
    this.options = options;

    // Create a CodeMirror instance
    this.cm = CodeMirror(null!, {
      value: this.node.textContent,
      extraKeys: this.codeMirrorKeymap(),
      lineWrapping: true,
      lineNumbers: this.options.lineNumbers,
      styleSelectedText: true,
    } as CodeMirror.EditorConfiguration);

    // update mode
    this.updateMode();

    // The editor's outer node is our DOM representation
    this.dom = this.cm.getWrapperElement();

    // theming
    this.dom.classList.add('pm-code-editor');
    this.dom.classList.add('pm-block-border-color');
    if (this.options.classes) {
      this.options.classes.forEach(className => this.dom.classList.add(className));
    }
    if (options.firstLineMeta) {
      this.dom.classList.add('CodeMirror-firstLineMeta');
      this.cm.addLineClass(0, 'text', 'pm-chunk-header-background-color pm-light-text-color pm-block-border-color');
    }

    // CodeMirror needs to be in the DOM to properly initialize, so
    // schedule it to update itself
    setTimeout(() => {
      this.cm.refresh();
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

  public update(node: ProsemirrorNode) {
    if (node.type !== this.node.type) {
      return false;
    }
    this.node = node;
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
    const lang = this.options.lang(this.node, this.cm.getValue());
    const mode = lang ? modeForLang(lang) : null;
    if (mode !== this.cm.getOption('mode')) {
      this.cm.setOption('mode', mode);
    }
  }

  private codeMirrorKeymap() {
    const view = this.view;
    const mod = /Mac/.test(navigator.platform) ? 'Cmd' : 'Ctrl';

    // exit code block
    const exitBlock = () => {
      if (exitCode(view.state, view.dispatch)) {
        view.focus();
      }
    };

    // Note: normalizeKeyMap not declared in CodeMirror types
    // so we cast to any
    return (CodeMirror as any).normalizeKeyMap({
      Up: () => this.arrowMaybeEscape('line', -1),
      Left: () => this.arrowMaybeEscape('char', -1),
      Down: () => this.arrowMaybeEscape('line', 1),
      Right: () => this.arrowMaybeEscape('char', 1),
      Backspace: () => this.backspaceMaybeDeleteNode(),
      // undo/redo keys are technically rebindable in the parent
      // editor so we may need a way to propagate the rebinding here
      [`${mod}-Z`]: () => undo(view.state, view.dispatch),
      [`Shift-${mod}-Z`]: () => redo(view.state, view.dispatch),
      [`${mod}-Y`]: () => redo(view.state, view.dispatch),
      [`${mod}-A`]: () => selectAll(view.state, view.dispatch, view),
      'Ctrl-Enter': exitBlock,
      'Shift-Enter': exitBlock,
      [`${mod}-Enter`]: exitBlock,
    });
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
    this.view.focus();
    const targetPos = this.getPos() + (dir < 0 ? 0 : this.node.nodeSize);
    const selection = Selection.near(this.view.state.doc.resolve(targetPos), dir);
    this.view.dispatch(this.view.state.tr.setSelection(selection).scrollIntoView());
    this.view.focus();
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

function arrowHandler(dir: 'up' | 'down' | 'left' | 'right' | 'forward' | 'backward', nodeTypes: string[]) {
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

function modeForLang(lang: string) {
  if (kModeMap.hasOwnProperty(lang)) {
    return kModeMap[lang];
  } else {
    return null;
  }
}
