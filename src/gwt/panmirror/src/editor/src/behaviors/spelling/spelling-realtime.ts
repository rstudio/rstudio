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
import { kAddToHistoryTransaction } from "../../api/transaction";

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

const kUpdateSpellingTransaction = 'realtimeSpelling';

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
          if (tr.getMeta(kUpdateSpellingTransaction)) {

            const { from = null, to = null } = tr.getMeta(kUpdateSpellingTransaction);
            if (from && to) {
              old = old.map(tr.mapping, tr.doc);
              old = old.remove(old.find(from, to));
              old = old.add(tr.doc, this.spellingDecorations(newState, ui.spelling, from, to));
              return old;
            } else {
              return DecorationSet.create(newState.doc, this.spellingDecorations(newState, ui.spelling));
            }


          } else if (tr.docChanged) {
            // TODO: incremental
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
          contextmenu: spellingSuggestionContextMenuHandler(ui)
        }
      },
    });

    // set excluded marks
    this.excluded = excluded;

    // trigger realtime spell check on initial focus
    const focusUnsubscribe = events.subscribe(FocusEvent, () => {
      if (this.view && !this.intialized) {
        this.intialized = true;
        updateSpellcheck(this.view);
      }
      focusUnsubscribe();
    });

  }

  private spellingDecorations(state: EditorState, spelling: EditorUISpelling, from?: number | null, to?: number | null): Decoration[] {

    // auto-initialize if we ever have focus (in case our FocusEvent somehow fails
    // to fire, we don't want to be stuck w/o spell-checking)
    if (this.view?.hasFocus()) {
      this.intialized = true;
    }

    // no-op if we aren't intialized
    if (!this.intialized) {
      return [];
    }

    // defaults from from and to
    from = from || 2;
    to = to || null;

    const decorations: Decoration[] = [];

    const words = getWords(state, from, to, spelling.breakWords, this.excluded);

    while (words.hasNext()) {
      const word = words.next()!;
      if (word.end !== state.selection.head) { // exclude words w/ active cursor
        const wordText = state.doc.textBetween(word.start, word.end);
        if (!spelling.checkWord(spellcheckerWord(wordText))) {
          decorations.push(Decoration.inline(word.start, word.end, { class: 'pm-spelling-error' }));
        }
      }
    }

    return decorations;
  }
}

function spellingSuggestionContextMenuHandler(ui: EditorUI) {

  return (view: EditorView, event: Event) => {

    if (event.target && event.target instanceof Node) {

      // alias schema
      const schema = view.state.schema;

      // find the spelling decoration at this position (if any)
      const pos = view.posAtDOM(event.target, 0);
      const deco = realtimeSpellingKey.getState(view.state)!.find(pos, pos);
      if (deco.length) {

        // get word
        const { from, to } = deco[0];
        const word = spellcheckerWord(view.state.doc.textBetween(from, to));

        // get suggetions 
        const kMaxSuggetions = 5;
        const suggestions = ui.spelling.suggestionList(word);

        // create menu w/ suggestions
        const menuItems: EditorMenuItem[] = suggestions.slice(0, kMaxSuggetions).map(suggestion => {
          suggestion = editorWord(suggestion);
          return {
            text: suggestion,
            exec: () => {
              const tr = view.state.tr;
              tr.setSelection(TextSelection.create(tr.doc, from, to));
              tr.replaceSelectionWith(schema.text(suggestion), true);
              setTextSelection(from + suggestion.length)(tr);
              tr.setMeta(kUpdateSpellingTransaction, { from, to: from + suggestion.length });
              view.dispatch(tr);
              view.focus();
            }
          };
        });
        if (menuItems.length) {
          menuItems.push({ separator: true });
        }

        // add other menu actions
        menuItems.push(
          {
            text: ui.context.translateText('Ignore Word'),
            exec: () => {
              ui.spelling.ignoreWord(word);
              updateSpellcheck(view);
              view.focus();
            }
          },
          { separator: true },
          {
            text: ui.context.translateText('Add to Dictionary'),
            exec: () => {
              ui.spelling.addToDictionary(word);
              updateSpellcheck(view);
              view.focus();
            }
          }
        );

        // show context menu
        const { clientX, clientY } = event as MouseEvent;
        ui.display.showContextMenu!(menuItems, clientX, clientY);

        // prevent default handling
        event.stopPropagation();
        event.preventDefault();
        return true;
      }
    }

    return false;
  };
}

function updateSpellcheck(view: EditorView, from?: number | null, to?: number | null) {
  const tr = view.state.tr;
  setUpdateSpellingTransaction(tr, from, to);
  view.dispatch(tr);
}

function setUpdateSpellingTransaction(tr: Transaction, from?: number | null, to?: number | null) {
  tr.setMeta(kUpdateSpellingTransaction, { from, to });
  tr.setMeta(kAddToHistoryTransaction, false);
}
