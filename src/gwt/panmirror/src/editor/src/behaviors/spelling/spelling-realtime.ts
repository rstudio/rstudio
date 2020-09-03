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
import { DecorationSet, EditorView, Decoration, DecorationAttrs } from "prosemirror-view";
import { AddMarkStep, RemoveMarkStep } from "prosemirror-transform";
import { ChangeSet } from "prosemirror-changeset";

import { setTextSelection } from "prosemirror-utils";

import { FocusEvent } from '../../api/event-types';
import { PandocMark } from "../../api/mark";
import { EditorUISpelling, kCharClassWord } from "../../api/spelling";
import { EditorEvents } from "../../api/events";
import { kAddToHistoryTransaction } from "../../api/transaction";
import { EditorUI, EditorMenuItem } from "../../api/ui";

import { excludedMarks, getWords, spellcheckerWord, findBeginWord, findEndWord, charAt } from "./spelling";

const kUpdateSpellingTransaction = 'updateSpelling';
const kInvalidateSpellingWordTransaction = 'invalidateSpellingWord';
const kSpellingErrorClass = 'pm-spelling-error';

const realtimeSpellingKey = new PluginKey<DecorationSet>('spelling-realtime-plugin');


export function realtimeSpellingPlugin(
  schema: Schema,
  marks: readonly PandocMark[],
  ui: EditorUI,
  events: EditorEvents) {
  return new RealtimeSpellingPlugin(excludedMarks(schema, marks), ui, events);
}

export function invalidateAllWords(view: EditorView) {
  updateSpelling(view);
}

export function invalidateWord(view: EditorView, word: string) {
  const tr = view.state.tr;
  tr.setMeta(kInvalidateSpellingWordTransaction, word);
  tr.setMeta(kAddToHistoryTransaction, false);
  view.dispatch(tr);
}

class RealtimeSpellingPlugin extends Plugin<DecorationSet> {

  // track whether we've ever had the focus (don't do any spelling operaitons until then)
  private hasBeenFocused = true;

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

          // if we somehow manage to get focus w/o our FocusEvent (below) being called then also 
          // flip the hasBeenFocused bit here
          if (this.view?.hasFocus()) {
            this.hasBeenFocused = true;
          }

          // don't continue if either realtime spelling is disabled or we have never been focused
          if (!this.ui.spelling.realtimeEnabled() || !this.hasBeenFocused) {
            return DecorationSet.empty;
          }

          if (tr.getMeta(kUpdateSpellingTransaction)) {

            // explicit update request invalidates any existing decorations (this can happen when
            // we get focus for the very firs time or when the main or secondary dictionaries change)
            return DecorationSet.create(newState.doc, spellingDecorations(newState, ui.spelling, excluded));

          } else if (tr.getMeta(kInvalidateSpellingWordTransaction)) {

            // for word invalidations we search through the decorations and remove words that match
            const word = tr.getMeta(kInvalidateSpellingWordTransaction) as string;

            // find decorations that have this word and remove them
            const wordDecos = old.find(undefined, undefined, spec => spec.word === word);

            // return decorators w/ those words removed
            return old.remove(wordDecos);

          } else if (tr.docChanged) {

            // perform an incremental update of spelling decorations (invalidate and re-scan
            // for decorations in changed ranges)

            // start w/ previous state
            let decos = old;

            // create change set from transaction
            let changeSet = ChangeSet.create(oldState.doc);
            changeSet = changeSet.addSteps(newState.doc, tr.mapping.maps);

            // collect ranges that had mark changes
            const markRanges: Array<{ from: number, to: number }> = [];
            for (const step of tr.steps) {
              if (step instanceof AddMarkStep || step instanceof RemoveMarkStep) {
                const markStep = step as any;
                markRanges.push({ from: markStep.from, to: markStep.to });
              }
            }

            // remove ranges = mark ranges + deleted ranges
            const removeRanges = markRanges.concat(changeSet.changes.map(change =>
              ({ from: change.fromA, to: change.toA })
            ));

            // remove decorations from deleted ranges (expanding ranges to word boundaries)
            for (const range of removeRanges) {
              const fromPos = findBeginWord(oldState, range.from, ui.spelling.classifyCharacter);
              const toPos = findEndWord(oldState, range.to, ui.spelling.classifyCharacter);
              decos = decos.remove(decos.find(fromPos, toPos));
            }

            // map decoration positions to new document
            decos = decos.map(tr.mapping, tr.doc);

            // add ranges = mark ranges + inserted ranges
            const addRanges = markRanges.concat(changeSet.changes.map(change =>
              ({ from: change.fromB, to: change.toB })
            ));

            // scan inserted ranges for spelling decorations (don't need to find word boundaries 
            // b/c spellingDecorations already does that)
            for (const range of addRanges) {
              decos = decos.add(tr.doc, spellingDecorations(newState, ui.spelling, excluded, true, range.from, range.to));
            }

            // return decorators
            return decos;

          } else if (tr.selectionSet) {

            // if we had previously suppressed a decoration due to typing at the cursor, restore it
            // whenever the selection changes w/o the doc changing

            // start with previous state
            let decos = old;

            // find any special 'at cursor' errors
            const cursorDecos = decos.find(undefined, undefined, spec => !!spec.cursor);
            if (cursorDecos.length) {

              // there will only be one cursor, capture it's position then remove it
              const word = cursorDecos[0].spec.word as string;
              const { from, to } = cursorDecos[0];
              decos = decos.remove(cursorDecos);

              // add it back in as a real spelling error
              decos = decos.add(tr.doc, [Decoration.inline(
                from,
                to,
                { class: kSpellingErrorClass },
                { word })
              ]);
            }

            // return decorators
            return decos;

          } else {

            // no content or selection change, return old w/o mapping
            return old;

          }

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

    // trigger update on first focus
    const focusUnsubscribe = events.subscribe(FocusEvent, () => {
      if (this.view) {
        focusUnsubscribe();
        this.hasBeenFocused = true;
        updateSpelling(this.view);
      }
    });
  }
}

function spellingDecorations(
  state: EditorState,
  spelling: EditorUISpelling,
  excluded: MarkType[],
  excludeCursor = false,
  from = -1,
  to = -1
): Decoration[] {

  // break words
  const words = getWords(state, from, to, spelling, excluded);

  // spell check and return decorations for misspellings
  const decorations: Decoration[] = [];
  while (words.hasNext()) {
    const word = words.next()!;
    const wordText = state.doc.textBetween(word.start, word.end);
    const wordCheck = spellcheckerWord(wordText);
    if (!spelling.checkWord(wordCheck)) {
      const attrs: DecorationAttrs = {};
      const spec: { [key: string]: any } = {
        word: wordCheck
      };
      if (excludeCursor && state.selection.head > word.start && state.selection.head <= word.end) {
        spec.cursor = true;
      } else {
        attrs.class = kSpellingErrorClass;
      }
      decorations.push(Decoration.inline(word.start, word.end, attrs, spec));
    }
  }
  return decorations;
}

function spellingSuggestionContextMenuHandler(ui: EditorUI) {

  return (view: EditorView, event: Event) => {

    if (!ui.display.showContextMenu) {
      return false;
    }

    // helper to create a menu action
    const menuAction = (text: string, action: VoidFunction) => {
      return {
        text,
        exec: () => {
          action();
          view.focus();
        }
      };
    };

    // helper to show a context menu and prevetn further event handling
    const showContextMenu = (menuItems: EditorMenuItem[]) => {
      // show the menu
      const { clientX, clientY } = event as MouseEvent;
      ui.display.showContextMenu!(menuItems, clientX, clientY);

      // prevent default handling
      event.stopPropagation();
      event.preventDefault();
    };

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
          return {
            text: suggestion,
            exec: () => {
              const tr = view.state.tr;
              tr.setSelection(TextSelection.create(tr.doc, from, to));
              const marks = tr.selection.$from.marks();
              tr.replaceSelectionWith(schema.text(suggestion, marks), false);
              setTextSelection(from + suggestion.length)(tr);
              view.dispatch(tr);
              view.focus();
            }
          };
        });
        if (menuItems.length) {
          menuItems.push({ separator: true });
        }

        menuItems.push(menuAction(ui.context.translateText('Ignore All'), () => ui.spelling.ignoreWord(word)));
        menuItems.push({ separator: true });
        menuItems.push(menuAction(ui.context.translateText('Add to Dictionary'), () => ui.spelling.addToDictionary(word)));

        // show context menu
        showContextMenu(menuItems);
        return true;
      }

      // find the word at this position and see if it's ignored. if so provide an unignore context menu
      const classify = ui.spelling.classifyCharacter;
      const mouseEvent = event as MouseEvent;
      const clickPos = view.posAtCoords({ left: mouseEvent.clientX, top: mouseEvent.clientY });
      if (clickPos) {
        const ch = charAt(view.state.doc, clickPos.pos);
        if (classify(ch) === kCharClassWord) {
          const from = findBeginWord(view.state, clickPos.pos, classify);
          const to = findEndWord(view.state, clickPos.pos, classify);
          const word = spellcheckerWord(view.state.doc.textBetween(from, to));
          if (ui.spelling.isWordIgnored(word)) {
            showContextMenu([
              menuAction(`${ui.context.translateText('Unignore')} \'${word}\'`, () => ui.spelling.unignoreWord(word))
            ]);
            return true;
          }
        }
      }
    }

    return false;
  };
}

function updateSpelling(view: EditorView) {
  const tr = view.state.tr;
  tr.setMeta(kUpdateSpellingTransaction, true);
  tr.setMeta(kAddToHistoryTransaction, false);
  view.dispatch(tr);
}
