/*
 * find.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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
import { Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';
import { DecorationSet, Decoration, EditorView } from 'prosemirror-view';
import { Node as ProsemirrorNode } from 'prosemirror-model';

import { mergedTextNodes } from '../api/text';

const key = new PluginKey<DecorationSet>('search-plugin');

interface FindResult {
  from: number;
  to: number;
}

// note that we manage search state within instance variables so that each client in a 
// collaborative editing scenario can have it's own find/replace state
class FindPlugin extends Plugin<DecorationSet> {

  private results: FindResult[] = [];
  private activeResult: number = -1;
  private term: string = "";
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
              return this.resultDecorations(tr.doc);
            } else if (tr.docChanged) {
              return old.map(tr.mapping, tr.doc);
            } else {
              return old;
            }
          },
        },
        props: {
          decorations: (state: EditorState) => {
            return key.getState(state);
          },
        },
    });    
  }

  public find(term: string, options: FindOptions) {
    return (state: EditorState<any>, dispatch?: ((tr: Transaction<any>) => void)) => {
      if (dispatch) {
        this.resetState();
        this.term = options.regex ? term.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&') : term;
        this.options = options;
        this.updateResults(state.doc);
        this.updateView(state, dispatch);
      }
      return true;
    };
  }

  public findAgain() {
    return (state: EditorState<any>, dispatch?: ((tr: Transaction<any>) => void)) => {
      if (dispatch) {
        this.updateResults(state.doc);
        this.updateView(state, dispatch);
      }
      return true;
    };
  }

  public haveResults() {
    return this.results.length > 0;
  }

  public findNext() {
    return (state: EditorState<any>, dispatch?: ((tr: Transaction<any>) => void)) => {
      
      // not active with no results
      if (this.results.length === 0) {
        return false;
      }

      // advance (return false if we are at the end and not wrapping)
      let nextResult = this.activeResult + 1;
      if (nextResult >= this.results.length) {
        if (!this.options.wrap) {
          return false;
        } else {
          nextResult = 0;
        }
      }
      
      // dispatch if requested
      if (dispatch) {
        this.activeResult = nextResult;
        this.updateView(state, dispatch);
      }

      return true;
    };
  }

  public findPrevious() {
    return (state: EditorState<any>, dispatch?: ((tr: Transaction<any>) => void)) => {
      
      // not active with no results
      if (this.results.length === 0) {
        return false;
      }

      // advance (return false if we are at the end and not wrapping)
      let prevResult = this.activeResult - 1;
      if (prevResult < 0) {
        if (!this.options.wrap) {
          return false;
        } else {
          prevResult = this.results.length - 1;
        }
      }
      
      // dispatch if requested
      if (dispatch) {
        this.activeResult = prevResult;
        this.updateView(state, dispatch);
      }

      return true;
    };
  }

  public replace(text: string) {
    return (state: EditorState<any>, dispatch?: ((tr: Transaction<any>) => void)) => {
      
      if (this.activeResult === -1) {
        return false;
      }

      if (dispatch) {
        // determine result to replace
        const { from, to } = this.results[this.activeResult];

        // do the replacement
        const tr = state.tr;
        tr.insertText(text, from, to);
        dispatch(tr);
      }

      return true;
    };
  }
  

  public clear() {
    return (state: EditorState<any>, dispatch?: ((tr: Transaction<any>) => void)) => {
      if (dispatch) {
        this.resetState();
        this.updateResults(state.doc);
        this.updateView(state, dispatch);
      }
      return true;
    };
  }

  private resetState() {
    this.term = "";
    this.options = {};
    this.results = [];
    this.activeResult = -1;
    this.updating = false;
  }


  private updateView(state: EditorState, dispatch: ((tr: Transaction<any>) => void)) {
    this.updating = true;
    dispatch(state.tr);
    this.updating = false;
  }

  
  private updateResults(doc: ProsemirrorNode) {
    
    // determine start loc for active result
    let startLoc = 0;
    if (this.activeResult !== -1) {
      startLoc = this.results[this.activeResult].to;
    }

    // clear results
    this.results = [];
    this.activeResult = -1;

    // bail if no search term
    if (this.term.length === 0) {
      return DecorationSet.empty;
    }

    // perform search and populate results
    const textNodes = mergedTextNodes(doc);
    textNodes.forEach(textNode => {
      const search = new RegExp(this.term, !this.options.caseSensitive ? 'gui' : 'gu');
      let m;
      // eslint-disable-next-line no-cond-assign
      while ((m = search.exec(textNode.text))) {
        if (m[0] === '') {
          break;
        }
        const result = {
          from: textNode.pos + m.index,
          to: textNode.pos + m.index + m[0].length,
        };
        this.results.push(result);
        if (this.activeResult === -1 && result.from >= startLoc) {
          this.activeResult = this.results.length - 1;
        }
      }
    });
  }


  private resultDecorations(doc: ProsemirrorNode) : DecorationSet {
  
     // build array of decorations
     const decos = this.results.map((deco, index) => {
        const cls = index === this.activeResult ? 'pm-find-text-active' : 'pm-find-text';
        return Decoration.inline(deco.from, deco.to, { class: cls })
    });

    // return as decoration set
    return decos.length ? DecorationSet.create(doc, decos) : DecorationSet.empty;
  }


};

const extension: Extension = {
  
  plugins: () => {
    return [
      new FindPlugin()
    ];
  }

};


// TODO: scroll into view for active result

// TODO: findAgain and wrap behavior
// TODO: replace and wrap behavior
// TODO: replaceAll and wrap behavior


export interface FindOptions {
  regex?: boolean;
  caseSensitive?: boolean;
  wrap?: boolean;
}


export function find(view: EditorView, term: string, options: FindOptions) {
  const state = view.state;
  const plugin = key.get(state) as FindPlugin;
  plugin.find(term, options)(state, view.dispatch);
  return plugin.haveResults();
}

export function canFindNext(view: EditorView) {
  const state = view.state;
  const plugin = key.get(state) as FindPlugin;
  return plugin.findNext()(state);
}

export function findNext(view: EditorView) {
  const state = view.state;
  const plugin = key.get(state) as FindPlugin;
  plugin.findNext()(state, view.dispatch);
}

export function canFindPrevious(view: EditorView) {
  const state = view.state;
  const plugin = key.get(state) as FindPlugin;
  return plugin.findPrevious()(state);
}

export function findPrevious(view: EditorView) {
  const state = view.state;
  const plugin = key.get(state) as FindPlugin;
  plugin.findPrevious()(state, view.dispatch);
}

export function canReplace(view: EditorView) {
  const state = view.state;
  const plugin = key.get(state) as FindPlugin;
  return plugin.haveResults();
}

export function replace(view: EditorView, text: string) {
  const state = view.state;
  const plugin = key.get(state) as FindPlugin;
  plugin.replace(text)(state, view.dispatch);
  plugin.findAgain()(view.state, view.dispatch);
  return plugin.haveResults();
}

export function replaceAll(view: EditorView, text: string) {
  while (replace(view, text)) {}
}

export default extension;