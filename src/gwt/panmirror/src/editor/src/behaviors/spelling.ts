/*
 * spelling.ts
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

import { MarkType, Schema } from 'prosemirror-model';
import { EditorView, DecorationSet, Decoration } from "prosemirror-view";
import { TextSelection, Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';
import { setTextSelection } from 'prosemirror-utils';

import { EditorWordSource, EditorWordRange, EditorAnchor, EditorRect, EditorSpellingDoc, EditorUISpelling } from "../api/spelling";
import { TextWithPos } from "../api/text";
import { scrollIntoView } from '../api/scroll';
import { ExtensionContext } from '../api/extension';
import { FocusEvent } from '../api/event-types';
import { PandocMark } from '../api/mark';
import { EditorEvents } from '../api/events';
import { kAddToHistoryTransaction, kInitRealtimeSpellingTransaction } from '../api/transaction';

// TODO: excluded marktypes
// TODO: words w/ apostrophies marked as misspelled
// TODO: context menu
// TODO: themed underline color
// TODO: more efficient / incremntal chekcing
// TODO: implement the rest of the TypeSpellChecker.Context (where does this play into viz mode?)

const extension = (context: ExtensionContext) => {
  return {
    plugins: (schema: Schema) => {
      return [
        new SpellingDocPlugin(),
        new SpellingRealtimePlugin(schema, context.ui.spelling, context.events)
      ];
    }
  };
};

export function getSpellingDoc(
  view: EditorView,
  marks: readonly PandocMark[],
  wordBreaker: (text: string) => EditorWordRange[]
): EditorSpellingDoc {

  // alias schema 
  const schema = view.state.schema;

  // intialize marks we don't want to check
  const excludedMarks = marks
    .filter(mark => mark.noSpelling)
    .map(mark => schema.marks[mark.name]);

  // check begin
  spellingDocPlugin(view.state).checkBegin();

  return {

    getWords: (start: number, end: number | null): EditorWordSource => {
      return getWords(
        view.state,
        start,
        end,
        wordBreaker,
        excludedMarks
      );
    },

    createAnchor: (pos: number): EditorAnchor => {
      return spellingDocPlugin(view.state).createAnchor(pos);
    },

    shouldCheck: (_wordRange: EditorWordRange): boolean => {
      return true;
    },

    setSelection: (wordRange: EditorWordRange) => {
      const tr = view.state.tr;
      tr.setSelection(TextSelection.create(tr.doc, wordRange.start, wordRange.end));
      view.dispatch(tr);
    },

    getText: (wordRange: EditorWordRange): string => {
      const word = view.state.doc.textBetween(wordRange.start, wordRange.end);
      return spellcheckerWord(word);
    },

    replaceSelection: (text: string) => {
      const tr = view.state.tr;
      text = editorWord(text);
      tr.replaceSelectionWith(view.state.schema.text(text), true);
      view.dispatch(tr);
    },

    getCursorPosition: (): number => {
      return view.state.selection.head;
    },

    getSelectionStart: (): number => {
      return view.state.selection.from;
    },

    getSelectionEnd: (): number => {
      return view.state.selection.to;
    },

    getCursorBounds: (): EditorRect => {

      const fromCoords = view.coordsAtPos(view.state.selection.from);
      const toCoords = view.coordsAtPos(view.state.selection.to);

      return {
        x: Math.min(fromCoords.left, toCoords.left),
        y: fromCoords.top,
        width: Math.abs(fromCoords.left - toCoords.left),
        height: toCoords.bottom - fromCoords.top
      };
    },

    moveCursorNearTop: () => {
      scrollIntoView(view, view.state.selection.from, false, undefined, 100);
    },

    dispose: () => {
      spellingDocPlugin(view.state).checkEnd(view);
    }

  };
}

// companion plugin for SpellingDoc provided above (shows 'fake' selection during
// interactive spell check dialog and maintains anchor position(s) across 
// transactions that occur while the dialog/doc is active)
const spellingDocKey = new PluginKey<DecorationSet>('spelling-doc-plugin');

function spellingDocPlugin(state: EditorState) {
  return spellingDocKey.get(state) as SpellingDocPlugin;
}

class SpellingDocPlugin extends Plugin<DecorationSet> {

  private checking = false;
  private anchors: SpellingAnchor[] = [];

  constructor() {
    super({
      key: spellingDocKey,
      state: {
        init: () => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {

          if (this.checking) {

            // map anchors
            this.anchors.forEach(anchor => {
              anchor.setPosition(tr.mapping.map(anchor.getPosition()));
            });

            // show selection 
            if (!tr.selection.empty) {
              return DecorationSet.create(
                tr.doc,
                [Decoration.inline(tr.selection.from, tr.selection.to, { class: 'pm-selected-text' })]
              );
            }
          }

          return DecorationSet.empty;
        },
      },
      props: {
        decorations: (state: EditorState) => {
          return spellingDocKey.getState(state);
        },
      },
    });
  }

  public checkBegin() {
    this.checking = true;
  }

  public createAnchor(pos: number) {
    const anchor = new SpellingAnchor(pos);
    this.anchors.push(anchor);
    return anchor;
  }

  public checkEnd(view: EditorView) {

    this.checking = false;
    this.anchors = [];

    if (!view.state.selection.empty) {
      const tr = view.state.tr;
      setTextSelection(tr.selection.to)(tr);
      view.dispatch(tr);
    }
  }
}

class SpellingAnchor implements EditorAnchor {

  private pos = 0;

  constructor(pos: number) {
    this.pos = pos;
  }

  public getPosition() {
    return this.pos;
  }

  public setPosition(pos: number) {
    this.pos = pos;
  }
}


const spellingRealtimeKey = new PluginKey<DecorationSet>('spelling-realtime-plugin');

function spellingRealtimePlugin(state: EditorState) {
  return spellingRealtimeKey.get(state) as SpellingRealtimePlugin;
}

class SpellingRealtimePlugin extends Plugin<DecorationSet> {

  private view: EditorView | null = null;
  private intialized = false;

  constructor(schema: Schema, spelling: EditorUISpelling, events: EditorEvents) {

    /*
    // intialize marks we don't want to check
    const excludedMarks = marks
      .filter(mark => mark.noSpelling)
      .map(mark => schema.marks[mark.name]);
    */

    super({
      key: spellingRealtimeKey,
      view: (view: EditorView) => {
        this.view = view;
        return {};
      },
      state: {
        init: (_config, state: EditorState) => {
          return DecorationSet.create(state.doc, this.spellingDecorations(state, spelling));
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          if (tr.docChanged || tr.getMeta(kInitRealtimeSpellingTransaction)) {
            return DecorationSet.create(newState.doc, this.spellingDecorations(newState, spelling));
          } else {
            return old;
          }

          // transactionsChangeSet

          // modify 'old' to remove any decorations that were in ranges that were either removed or modified
          // then map: old = old.map(tr.mapping, tr.doc);

          // look at modified and newly inserted ranges from the ChangeSet
          // spell check those nodes and add decorations

          // generally, I only get positions so I will need to dedude the enclosing node(s)

          // we could discover the "range" by walking forwards and backwards from the changed range 
          // (stop at block end)

          // when walking, find a character or "invalidator/boundary":
          //     space
          //     block level boundary
          //     disqualifying mark

          // realtime: don't spell check words that are at the cursor
          return old;
        }
      },
      props: {
        decorations: (state: EditorState) => {
          return spellingRealtimeKey.getState(state);
        },
      },
    });

    // trigger realtime spell check on initial focus
    const focusUnsubscribe = events.subscribe(FocusEvent, () => {
      if (this.view && !this.intialized) {
        this.intialized = true;
        const tr = this.view.state.tr;
        tr.setMeta(kInitRealtimeSpellingTransaction, true);
        tr.setMeta(kAddToHistoryTransaction, false);
        this.view.dispatch(tr);
      }
      focusUnsubscribe();
    });

  }

  private spellingDecorations(state: EditorState, spelling: EditorUISpelling): Decoration[] {

    // auto-initialize if we ever have focus (in case our FocusEvent somehow fails
    // to fire, we don't want to be stuck w/o spell-checking)
    if (this.view?.hasFocus()) {
      this.intialized = true;
    }

    // no-op if we aren't intialized
    if (!this.intialized) {
      return [];
    }

    const start = performance.now();

    const decorations: Decoration[] = [];

    const words = getWords(state, 2, null, spelling.breakWords, []);

    let checked = 0;
    while (words.hasNext()) {
      const word = words.next()!;
      if (word.end !== state.selection.head) { // exclude words w/ active cursor
        checked++;
        const wordText = state.doc.textBetween(word.start, word.end);
        if (!spelling.checkWord(wordText)) {
          decorations.push(Decoration.inline(word.start, word.end, { class: 'pm-spelling-error' }));
        }
      }
    }

    const end = performance.now();

    // console.log('spell checked ' + checked + ' words in ' + (end - start) + 'ms');

    return decorations;
  }
}

function getWords(
  state: EditorState,
  start: number,
  end: number | null,
  wordBreaker: (text: string) => EditorWordRange[],
  excludedMarks: MarkType[]
): EditorWordSource {

  // provide default for end
  if (end === null) {
    end = state.doc.nodeSize - 2;
  }

  // examine every text node
  const textNodes: TextWithPos[] = [];
  state.doc.nodesBetween(start, end, (node, pos, parent) => {
    if (node.isText && !parent.type.spec.code) {
      // filter on marks where we shouldn't check spelling (e.g. url, code)
      if (!excludedMarks.some((markType: MarkType) => markType.isInSet(node.marks))) {
        textNodes.push({ text: node.textContent, pos });
      }
    }
  });

  // create word ranges
  const words: EditorWordRange[] = [];
  textNodes.forEach(text => {
    if (text.pos >= start && text.pos < end!) {
      words.push(...wordBreaker(text.text).map(wordRange => {
        return {
          start: text.pos + wordRange.start,
          end: text.pos + wordRange.end
        };
      }));
    }
  });

  // return iterator over word range
  return {
    hasNext: () => {
      return words.length > 0;
    },
    next: () => {
      if (words.length > 0) {
        return words.shift()!;
      } else {
        return null;
      }
    }
  };
}

function spellcheckerWord(word: string) {
  return word.replace(/’/g, '\'');
}

function editorWord(word: string) {
  return word.replace(/'/g, '’');
}



export default extension;