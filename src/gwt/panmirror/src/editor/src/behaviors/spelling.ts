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

import { Schema, MarkType } from 'prosemirror-model';
import { EditorView, DecorationSet, Decoration } from "prosemirror-view";
import { TextSelection, Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';

import { EditorWordBreaker, EditorWordSource, EditorWordRange, EditorAnchor, EditorRect, EditorSpellingDoc } from "../api/spelling";
import { TextWithPos } from "../api/text";
import { scrollIntoView } from '../api/scroll';
import { ExtensionContext } from '../api/extension';
import { setTextSelection } from 'prosemirror-utils';
import { PandocMark } from '../api/mark';

const extension = (context: ExtensionContext) => {
  return {
    plugins: (schema: Schema) => {
      return [new SpellingPlugin()];
    }
  };
};

export function getSpellingDoc(view: EditorView, marks: readonly PandocMark[], wordBreaker: EditorWordBreaker): EditorSpellingDoc {

  // alias schema 
  const schema = view.state.schema;

  // intialize marks we don't want to check
  const excludedMarks = marks
    .filter(mark => mark.noSpelling)
    .map(mark => schema.marks[mark.name]);

  // check begin
  spellingPlugin(view.state).checkBegin();

  return {

    getWords: (start: number, end: number | null): EditorWordSource => {

      // provide default for end
      if (end === null) {
        end = view.state.doc.nodeSize - 2;
      }

      // examine every text node
      const textNodes: TextWithPos[] = [];
      view.state.doc.nodesBetween(start, end, (node, pos, parent) => {
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
    },

    createAnchor: (pos: number): EditorAnchor => {
      return spellingPlugin(view.state).createAnchor(pos);
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
      return word.replace(/’/g, '\'');
    },

    getCursorPosition: (): number => {
      return view.state.selection.head;
    },

    replaceSelection: (text: string) => {
      const tr = view.state.tr;
      tr.replaceSelectionWith(view.state.schema.text(text), true);
      view.dispatch(tr);
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
      spellingPlugin(view.state).checkEnd(view);
    }

  };
}

const key = new PluginKey<DecorationSet>('spelling-plugin');


function spellingPlugin(state: EditorState) {
  return key.get(state) as SpellingPlugin;
}

class SpellingPlugin extends Plugin<DecorationSet> {

  private checking = false;
  private anchors: SpellingAnchor[] = [];

  constructor() {
    super({
      key,
      state: {
        init: () => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {

          // transactionsChangeSet

          // modify 'old' to remove any decorations that were in ranges that were either removed or modified
          // then map: old = old.map(tr.mapping, tr.doc);

          // look at modified and newly inserted ranges from the ChangeSet
          // spell check those nodes and add decorations

          // when walking, find a character or "invalidator/boundary":
          //     space
          //     block level boundary
          //     disqualifying mark

          // generally, I only get positions so I will need to dedude the enclosing node(s)

          // we could discover the "range" by walking forwards and backwards from the changed range 
          // (stop at block end)

          // realtime: don't spell check words that are at the cursor


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
      view: () => ({
        update: (view: EditorView, prevState: EditorState) => {
          // 
        },
      }),
      props: {
        decorations: (state: EditorState) => {
          return key.getState(state);
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

export default extension;