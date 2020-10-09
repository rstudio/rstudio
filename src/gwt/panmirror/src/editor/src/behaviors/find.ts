/*
 * find.ts
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

import { Extension } from '../api/extension';
import { Plugin, PluginKey, EditorState, Transaction, TextSelection } from 'prosemirror-state';
import { DecorationSet, Decoration, EditorView } from 'prosemirror-view';

import { mergedTextNodes } from '../api/text';
import { kAddToHistoryTransaction } from '../api/transaction';
import { scrollIntoView } from '../api/scroll';

const key = new PluginKey<DecorationSet>('find-plugin');

class FindPlugin extends Plugin<DecorationSet> {
  private term: string = '';
  private options: FindOptions = {};
  private updating: boolean = false;

  constructor() {
    super({
      key,
      state: {
        init: (_config: { [key: string]: any }, instance: EditorState) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          if (this.updating) {
            return this.resultDecorations(tr);
          } else {
            return DecorationSet.empty;
          }
        },
      },
      view: () => ({
        update: (view: EditorView, prevState: EditorState) => {
          if (this.isResultSelected(view.state)) {
            this.scrollToSelectedResult(view);
          }
        },
      }),
      props: {
        decorations: (state: EditorState) => {
          return key.getState(state);
        },
      },
    });
  }

  public find(term: string, options: FindOptions) {
    return (state: EditorState<any>, dispatch?: (tr: Transaction<any>) => void) => {
      if (dispatch) {
        this.term = !options.regex ? term.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&') : term;
        this.options = options;
        this.updateResults(state, dispatch);
      }
      return true;
    };
  }

  public matchCount(state: EditorState) {
    return key.getState(state)!.find().length;
  }

  public selectFirst() {
    return (state: EditorState<any>, dispatch?: (tr: Transaction<any>) => void) => {
      const decorations: Decoration[] = key.getState(state)!.find(0);
      if (decorations.length === 0) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;
        this.selectResult(tr, decorations[0]);
        this.withResultUpdates(() => {
          dispatch(tr);
        });
      }

      return true;
    };
  }

  public selectCurrent() {
    return this.selectNext(false);
  }

  public selectNext(afterSelection = true) {
    return (state: EditorState<any>, dispatch?: (tr: Transaction<any>) => void) => {
      const selectedText = state.doc.textBetween(state.selection.from, state.selection.to);
      const searchFrom = afterSelection
        ? this.matchesTerm(selectedText)
          ? state.selection.to + 1
          : state.selection.to
        : state.selection.from;

      const decorationSet = key.getState(state)!;
      let decorations: Decoration[] = decorationSet.find(searchFrom);
      if (decorations.length === 0) {
        // check for wrapping
        if (this.options.wrap) {
          const searchTo = this.matchesTerm(selectedText) ? state.selection.from - 1 : state.selection.from;
          decorations = decorationSet.find(0, searchTo);
          if (decorations.length === 0) {
            return false;
          }
          // no wrapping
        } else {
          return false;
        }
      }

      if (dispatch) {
        const tr = state.tr;
        this.selectResult(tr, decorations[0]);
        this.withResultUpdates(() => {
          dispatch(tr);
        });
      }
      return true;
    };
  }

  public selectPrevious() {
    return (state: EditorState<any>, dispatch?: (tr: Transaction<any>) => void) => {
      // sort out where we are searching up to
      const selectedText = state.doc.textBetween(state.selection.from, state.selection.to);
      const searchTo = this.matchesTerm(selectedText) ? state.selection.from - 1 : state.selection.from;

      // get all decorations up to the current selection
      const decorationSet = key.getState(state)!;
      let decorations: Decoration[] = decorationSet.find(0, searchTo);
      if (decorations.length === 0) {
        // handle wrapping
        if (this.options.wrap) {
          const searchFrom = this.matchesTerm(selectedText) ? state.selection.to + 1 : state.selection.to;
          decorations = decorationSet.find(searchFrom);
          if (decorations.length === 0) {
            return false;
          }
          // no wrapping
        } else {
          return false;
        }
      }

      // find the one closest to the beginning of the current selection
      if (dispatch) {
        // now we need to find the decoration with the largest from value
        const decoration = decorations.reduce((lastDecoration, nextDecoration) => {
          if (nextDecoration.from > lastDecoration.from) {
            return nextDecoration;
          } else {
            return lastDecoration;
          }
        });

        const tr = state.tr;
        this.selectResult(tr, decoration);
        this.withResultUpdates(() => {
          dispatch(tr);
        });
      }
      return true;
    };
  }

  public replace(text: string) {
    return (state: EditorState<any>, dispatch?: (tr: Transaction<any>) => void) => {
      if (!this.isResultSelected(state)) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;
        const selectionMarks = tr.selection.$from.marks();
        tr.replaceSelectionWith(state.schema.text(text, selectionMarks), false);
        this.withResultUpdates(() => {
          dispatch(tr);
        });
      }

      return true;
    };
  }

  public replaceAll(text: string) {
    return (state: EditorState<any>, dispatch?: (tr: Transaction<any>) => void) => {
      if (!this.hasTerm()) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;

        const decorationSet = key.getState(state)!;

        const decorations: Decoration[] = decorationSet.find(0);
        decorations.forEach(decoration => {
          const from = tr.mapping.map(decoration.from);
          const to = tr.mapping.map(decoration.to);
          tr.insertText(text, from, to);
        });
        this.withResultUpdates(() => {
          dispatch(tr);
        });
      }

      return true;
    };
  }

  public clear() {
    return (state: EditorState<any>, dispatch?: (tr: Transaction<any>) => void) => {
      if (dispatch) {
        this.term = '';
        this.options = {};
        this.updateResults(state, dispatch);
      }
      return true;
    };
  }

  private updateResults(state: EditorState, dispatch: (tr: Transaction<any>) => void) {
    this.withResultUpdates(() => {
      const tr = state.tr;
      tr.setMeta(kAddToHistoryTransaction, false);
      dispatch(tr);
    });
  }

  private resultDecorations(tr: Transaction): DecorationSet {
    // bail if no search term
    if (!this.hasTerm()) {
      return DecorationSet.empty;
    }

    // decorations to return
    const decorations: Decoration[] = [];

    // merge text nodes
    const textNodes = mergedTextNodes(tr.doc);

    textNodes.forEach(textNode => {
      const search = this.findRegEx();
      if (!search) {
        return;
      }

      let m;
      // tslint:disable-next-line no-conditional-assignment
      while ((m = search.exec(textNode.text))) {
        if (m[0] === '') {
          break;
        }
        const from = textNode.pos + m.index;
        const to = textNode.pos + m.index + m[0].length;
        const classes = ['pm-find-text'];
        if (from === tr.selection.from && to === tr.selection.to) {
          classes.push('pm-selected-text');
        }
        decorations.push(Decoration.inline(from, to, { class: classes.join(' ') }));
      }
    });

    // return as decoration set
    return decorations.length ? DecorationSet.create(tr.doc, decorations) : DecorationSet.empty;
  }

  private withResultUpdates(f: () => void) {
    this.updating = true;
    f();
    this.updating = false;
  }

  private selectResult(tr: Transaction, decoration: Decoration) {
    const selection = new TextSelection(tr.doc.resolve(decoration.from), tr.doc.resolve(decoration.to));
    return tr.setSelection(selection).scrollIntoView();
  }

  private isResultSelected(state: EditorState) {
    if (this.hasTerm()) {
      const selectedText = state.doc.textBetween(state.selection.from, state.selection.to);
      return this.matchesTerm(selectedText);
    } else {
      return false;
    }
  }

  private scrollToSelectedResult(view: EditorView) {
    scrollIntoView(view, view.state.selection.from, true, 350, 100);
  }

  private hasTerm() {
    return this.term.length > 0;
  }

  private matchesTerm(text: string) {
    if (this.hasTerm()) {
      const regex = this.findRegEx();
      if (regex) {
        return regex.test(text);
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  private findRegEx() {
    try {
      return new RegExp(this.term, !this.options.caseSensitive ? 'gui' : 'gu');
    } catch {
      return null;
    }
  }
}

const extension: Extension = {
  plugins: () => {
    return [new FindPlugin()];
  },
};

export interface FindOptions {
  regex?: boolean;
  caseSensitive?: boolean;
  wrap?: boolean;
}

export function find(view: EditorView, term: string, options: FindOptions): boolean {
  return findPlugin(view).find(term, options)(view.state, view.dispatch);
}

export function matchCount(view: EditorView): number {
  return findPlugin(view).matchCount(view.state);
}

export function selectFirst(view: EditorView): boolean {
  return findPlugin(view).selectFirst()(view.state, view.dispatch);
}

export function selectCurrent(view: EditorView): boolean {
  return findPlugin(view).selectCurrent()(view.state, view.dispatch);
}

export function selectNext(view: EditorView): boolean {
  return findPlugin(view).selectNext()(view.state, view.dispatch);
}

export function selectPrevious(view: EditorView): boolean {
  return findPlugin(view).selectPrevious()(view.state, view.dispatch);
}

export function replace(view: EditorView, text: string): boolean {
  return findPlugin(view).replace(text)(view.state, view.dispatch);
}

export function replaceAll(view: EditorView, text: string) {
  return findPlugin(view).replaceAll(text)(view.state, view.dispatch);
}

export function clear(view: EditorView): boolean {
  return findPlugin(view).clear()(view.state, view.dispatch);
}

export function findPluginState(state: EditorState): DecorationSet | null | undefined {
  return key.getState(state);
}

function findPlugin(view: EditorView): FindPlugin {
  return key.get(view.state) as FindPlugin;
}

export default extension;
