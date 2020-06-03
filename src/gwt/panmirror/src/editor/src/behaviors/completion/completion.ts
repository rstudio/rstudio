/*
 * completion.ts
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

import { Plugin, PluginKey, Transaction, Selection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { CompletionHandler, CompletionResult } from '../../api/completion';
import { EditorEvents, EditorEvent } from '../../api/events';

import { renderCompletionPopup, createCompletionPopup, destroyCompletionPopup } from './completion-popup';
import { canInsertNode } from '../../api/node';

export function completionExtension(handlers: readonly CompletionHandler[], events: EditorEvents) {
  return {
    plugins: () => [new CompletionPlugin(handlers, events)]
  };
}

interface CompletionState {
  handler?: CompletionHandler;
  result?: CompletionResult;
}


// TODO: popup positioning

// TODO: keyboard and mouse navigation/selection
// TODO: insertion (may need to return arbitrary transactions for /command)

// TODO: invalidation token for multiple concurrent requests 
// (including cancel existing)


const key = new PluginKey<CompletionState>('completion');

class CompletionPlugin extends Plugin<CompletionState> {
  
  private readonly scrollUnsubscribe: VoidFunction;
  private readonly completionPopup: HTMLElement;

  constructor(handlers: readonly CompletionHandler[], events: EditorEvents) {
    super({
      key,
      state: {
        init: () => ({}),
        apply: (tr: Transaction)  => {

          // selection only changes dismiss any active completion
          if (!tr.docChanged && tr.selectionSet) {
            return {};
          }

          // non empty selections don't have completions
          if (!tr.selection.empty) {
            return {};
          }

          // must be able to insert text
          const schema = tr.doc.type.schema;
          if (!canInsertNode(tr.selection, schema.nodes.text)) {
            return {};
          }

          // must not be in a code mark
          if (!!schema.marks.code.isInSet(tr.storedMarks || tr.selection.$from.marks())) {
            return {};
          }
          
          
          // calcluate text before cursor
          const textBefore = completionTextBeforeCursor(tr.selection);

          // check for a handler that can provide completions at the current selection
          for (const handler of handlers) {
            const result = handler.completions(textBefore, tr.selection);
            if (result) {
              return { handler, result };
            }
          }

          // no handler found
          return {};
        }
      },
      
      view: () => ({
        update: (view: EditorView) => {
          
          // if we have completions then show them
          const state = key.getState(view.state);
          if (state?.handler) {

            renderCompletionPopup(view, state.handler, state.result!, this.completionPopup)
              .then(this.showCompletions);

          // otherwise hide any visible popup
          } else {
            this.hideCompletions();
          }
          
        },

        destroy: () => {

          // unsubscribe from events
          this.scrollUnsubscribe();
          window.document.removeEventListener('focusin', this.focusChanged);

          // tear down the popup
          destroyCompletionPopup(this.completionPopup);

        },
      }),
    });

    // bind callback methods
    this.showCompletions = this.showCompletions.bind(this);
    this.hideCompletions = this.hideCompletions.bind(this);
    this.focusChanged = this.focusChanged.bind(this);

    // hide completions when we scroll
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.hideCompletions);

    // check for focus changes (e.g. dismiss when user clicks a menu)
    window.document.addEventListener('focusin', this.focusChanged);

    // create the popup, add it, and make it initially hidden
    this.completionPopup = createCompletionPopup();
    window.document.body.appendChild(this.completionPopup);
    this.hideCompletions();
  }

  // when a focus change occurs hide the popup if the popup itself isn't focused
  private focusChanged() {
    if (
      window.document.activeElement !== this.completionPopup &&
      !this.completionPopup.contains(window.document.activeElement)
    ) {
      this.hideCompletions();
    }
  }

  private showCompletions(show: boolean) {
    this.completionPopup.style.display =  show ? '' : 'none';
  }

  private hideCompletions() {
    this.showCompletions(false);
  }
}


// extract the text before the cursor, dealing with block separators and
// non-text leaf chracters (this is based on code in prosemirror-inputrules)
function completionTextBeforeCursor(selection: Selection, maxLength = 500) {
  const { $head } = selection;
  return $head.parent.textBetween(
    Math.max(0, $head.parentOffset - maxLength),  // start
    $head.parentOffset,                           // end
    undefined,                                    // block separator
    "\ufffc"                                      // leaf char
  );   
}
