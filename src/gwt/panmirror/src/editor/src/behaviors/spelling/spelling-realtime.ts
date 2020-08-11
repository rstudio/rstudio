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

// TODO: themed underline color

// TODO: gdocs style spelling text popup
// TODO: editing of document and user dictionary lists



import { Schema, MarkType } from "prosemirror-model";
import { Plugin, PluginKey, EditorState, Transaction, TextSelection } from "prosemirror-state";
import { DecorationSet, EditorView, Decoration, DecorationAttrs } from "prosemirror-view";
import { ChangeSet } from "prosemirror-changeset";

import { setTextSelection } from "prosemirror-utils";

import { FocusEvent } from '../../api/event-types';
import { PandocMark } from "../../api/mark";
import { EditorUISpelling } from "../../api/spelling";
import { EditorEvents } from "../../api/events";
import { kAddToHistoryTransaction } from "../../api/transaction";
import { EditorUI, EditorMenuItem } from "../../api/ui";

import { excludedMarks, getWords, spellcheckerWord, editorWord, findBeginWord, findEndWord } from "./spelling";
import { AddMarkStep, RemoveMarkStep } from "prosemirror-transform";

const kUpdateSpellingTransaction = 'updateSpelling';
const kSpellingErrorClass = 'pm-spelling-error';

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

          // check for disabled state
          if (this.disabled()) {
            return DecorationSet.empty;
          }

          if (tr.getMeta(kUpdateSpellingTransaction)) {

            // explicit update request invalidates any existing decorations (this can happen when
            // dictionaries change or when focus is restored after 'missing' a bunch of other
            // invalidations due to not beign the active tab)
            return DecorationSet.create(newState.doc, spellingDecorations(newState, ui.spelling, excluded));

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
              const { from, to } = cursorDecos[0];
              decos = decos.remove(cursorDecos);

              // add it back in as a real spelling error
              decos = decos.add(tr.doc, [Decoration.inline(from, to, { class: kSpellingErrorClass })]);
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

    // trigger update whenever we get the focus (updates are suspended while we aren't the active tab)
    events.subscribe(FocusEvent, () => {
      if (this.view) {
        this.hasBeenFocused = true;
        updateSpelling(this.view);
      }
    });
  }

  private disabled() {
    return !this.hasBeenFocused ||
      !this.ui.context.isActiveTab() ||
      !this.ui.spelling.realtimeEnabled();
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
    if (!spelling.checkWord(spellcheckerWord(wordText))) {
      const attrs: DecorationAttrs = {};
      const spec: { [key: string]: any } = {};
      if (excludeCursor && (state.selection.head === word.end)) {
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
              view.dispatch(tr);
              view.focus();
            }
          };
        });
        if (menuItems.length) {
          menuItems.push({ separator: true });
        }

        // add other menu actions
        const menuAction = (text: string, action: VoidFunction) => {
          return {
            text: ui.context.translateText(text),
            exec: () => {
              action();
              view.focus();
            }
          };
        };
        menuItems.push(menuAction('Ignore All', () => ui.spelling.ignoreWord(word)));
        menuItems.push({ separator: true });
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

function updateSpelling(view: EditorView) {
  const tr = view.state.tr;
  tr.setMeta(kUpdateSpellingTransaction, true);
  tr.setMeta(kAddToHistoryTransaction, false);
  view.dispatch(tr);
}
