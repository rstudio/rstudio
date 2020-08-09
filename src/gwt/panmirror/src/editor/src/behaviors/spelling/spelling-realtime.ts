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

import { setTextSelection } from "prosemirror-utils";

import { FocusEvent } from '../../api/event-types';
import { PandocMark } from "../../api/mark";
import { EditorUISpelling } from "../../api/spelling";
import { EditorEvents } from "../../api/events";
import { kAddToHistoryTransaction } from "../../api/transaction";

import { EditorUI, EditorMenuItem } from "../../api/ui";

import { excludedMarks, getWords, spellcheckerWord, editorWord } from "./spelling";

const kUpdateSpellingTransaction = 'updateSpelling';

const realtimeSpellingKey = new PluginKey<DecorationSet>('spelling-realtime-plugin');


export function realtimeSpellingPlugin(
  schema: Schema,
  marks: readonly PandocMark[],
  ui: EditorUI,
  events: EditorEvents) {
  return new RealtimeSpellingPlugin(excludedMarks(schema, marks), ui, events);
}

export function updateRealtimeSpelling(view: EditorView) {
  updateSpelling(view);
}

class RealtimeSpellingPlugin extends Plugin<DecorationSet> {

  private view: EditorView | null = null;
  private readonly ui: EditorUI;

  constructor(excluded: MarkType[], ui: EditorUI, events: EditorEvents) {

    super({
      key: realtimeSpellingKey,
      view: (view: EditorView) => {
        this.view = view;
        return {};
      },
      state: {
        init: (_config, state: EditorState) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {

          // collect explicit update request
          const update = tr.getMeta(kUpdateSpellingTransaction);

          // focused state is logical (either actually focused or the update is forcing that state)
          const focused = this.view?.hasFocus() || (update && update.focused);

          // check for disabled state
          if (this.disabled(focused)) {
            return DecorationSet.empty;
          }

          // update if this was either an explicit request or if the doc changed
          if (update || tr.docChanged) {

            // TODO: incremental (see below)
            return DecorationSet.create(newState.doc, spellingDecorations(newState, ui.spelling, excluded));

          } else {

            // TODO: handle cursor moved away from misspelled word
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

    // save reference to ui
    this.ui = ui;

    // trigger spelling update on focus
    events.subscribe(FocusEvent, () => {
      if (this.view) {
        updateSpelling(this.view, true);
      }
    });
  }

  private disabled(focused: boolean) {
    return !this.ui.spelling.realtimeEnabled() ||
      !this.ui.display.showContextMenu ||
      !focused;
  }

}

function spellingDecorations(
  state: EditorState,
  spelling: EditorUISpelling,
  excluded: MarkType[],
  from?: number,
  to?: number
): Decoration[] {

  const decorations: Decoration[] = [];

  const words = getWords(
    state,
    from || 2,
    to || null,
    spelling.breakWords,
    excluded
  );

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
              setUpdateSpellingTransaction(tr, true);
              view.dispatch(tr);
              view.focus();
            }
          };
        });
        if (menuItems.length) {
          menuItems.push({ separator: true });
        }

        const menuAction = (text: string, action: VoidFunction) => {
          return {
            text: ui.context.translateText(text),
            exec: () => {
              view.focus();
              setTimeout(action, 0);
            }
          };
        };

        // add other menu actions
        menuItems.push(menuAction('Ignore Word', () => ui.spelling.ignoreWord(word)));
        menuItems.push(menuAction('Add to Dictionary', () => ui.spelling.addToDictionary(word)));

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

function updateSpelling(view: EditorView, focused = false) {
  const tr = view.state.tr;
  setUpdateSpellingTransaction(tr, focused);
  view.dispatch(tr);
}

function setUpdateSpellingTransaction(tr: Transaction, focused = false) {
  tr.setMeta(kUpdateSpellingTransaction, { focused });
  tr.setMeta(kAddToHistoryTransaction, false);
}
