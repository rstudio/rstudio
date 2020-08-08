/*
 * spelling-realtime.ts
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

import { Schema, MarkType } from "prosemirror-model";
import { Plugin, PluginKey, EditorState, Transaction, TextSelection } from "prosemirror-state";
import { DecorationSet, EditorView, Decoration } from "prosemirror-view";

import { FocusEvent } from '../../api/event-types';
import { PandocMark } from "../../api/mark";
import { EditorUISpelling } from "../../api/spelling";
import { EditorEvents } from "../../api/events";
import { kInitRealtimeSpellingTransaction, kAddToHistoryTransaction } from "../../api/transaction";

import { excludedMarks, getWords, spellcheckerWord, editorWord } from "./spelling";
import { EditorUI, EditorMenuItem } from "../../api/ui";
import { setTextSelection } from "prosemirror-utils";

export function realtimeSpellingPlugin(
  schema: Schema,
  marks: readonly PandocMark[],
  ui: EditorUI,
  events: EditorEvents) {
  return new RealtimeSpellingPlugin(excludedMarks(schema, marks), ui, events);
}

const realtimeSpellingKey = new PluginKey<DecorationSet>('spelling-realtime-plugin');

class RealtimeSpellingPlugin extends Plugin<DecorationSet> {

  private view: EditorView | null = null;
  private intialized = false;
  private readonly excluded: MarkType[];

  constructor(excluded: MarkType[], ui: EditorUI, events: EditorEvents) {

    super({
      key: realtimeSpellingKey,
      view: (view: EditorView) => {
        this.view = view;
        return {};
      },
      state: {
        init: (_config, state: EditorState) => {
          return DecorationSet.create(state.doc, this.spellingDecorations(state, ui.spelling));
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          if (tr.docChanged || tr.getMeta(kInitRealtimeSpellingTransaction)) {
            return DecorationSet.create(newState.doc, this.spellingDecorations(newState, ui.spelling));
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
          return realtimeSpellingKey.getState(state);
        },
        handleDOMEvents: {
          contextmenu: (view: EditorView, event: Event) => {
            if (event.target && event.target instanceof Node) {
              const schema = view.state.schema;
              const pos = view.posAtDOM(event.target, 0);
              const deco = this.getState(view.state).find(pos, pos);
              if (deco.length) {
                const { from, to } = deco[0];
                const word = view.state.doc.textBetween(from, to);

                const kMaxSuggetions = 5;
                const suggestions = ui.spelling.suggestionList(spellcheckerWord(word));
                const menuItems: EditorMenuItem[] = suggestions.slice(0, kMaxSuggetions).map(suggestion => {
                  suggestion = editorWord(suggestion);
                  return {
                    text: suggestion,
                    exec: () => {
                      const tr = view.state.tr;
                      tr.setSelection(TextSelection.create(tr.doc, from, to));
                      tr.replaceSelectionWith(schema.text(suggestion), true);
                      setTextSelection(from + suggestion.length)(tr);
                      view.dispatch(tr);
                      view.focus();
                    }
                  };
                });
                if (menuItems.length) {
                  menuItems.push({ separator: true });
                }
                menuItems.push(
                  {
                    text: ui.context.translateText('Ignore Word'),
                    exec: () => {
                      ui.spelling.ignoreWord(word);
                      // TODO: invalidate
                    }
                  },
                  { separator: true },
                  {
                    text: ui.context.translateText('Add to Dictionary'),
                    exec: () => {
                      ui.spelling.addToDictionary(word);
                      // TODO: invalidate
                    }
                  }
                );

                const { clientX, clientY } = event as MouseEvent;
                ui.display.showContextMenu!(menuItems, clientX, clientY);

                event.stopPropagation();
                event.preventDefault();
                return true;
              }
            }

            return false;
          }
        }
      },


    });

    // set excluded marks
    this.excluded = excluded;

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

    const decorations: Decoration[] = [];

    const words = getWords(state, 2, null, spelling.breakWords, this.excluded);

    while (words.hasNext()) {
      const word = words.next()!;
      if (word.end !== state.selection.head) { // exclude words w/ active cursor
        const wordText = state.doc.textBetween(word.start, word.end);
        if (!spelling.checkWord(spellcheckerWord(wordText))) {
          decorations.push(Decoration.inline(word.start, word.end, { class: 'pm-spelling-error' }));
        }
      }
    }

    const end = performance.now();

    return decorations;
  }
}
