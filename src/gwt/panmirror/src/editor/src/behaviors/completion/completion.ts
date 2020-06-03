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

import { Node as ProsemirrorNode, Slice, Fragment } from 'prosemirror-model';
import { Plugin, PluginKey, Transaction, Selection, TextSelection,} from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { CompletionHandler, CompletionResult } from '../../api/completion';
import { EditorEvents, EditorEvent } from '../../api/events';

import { renderCompletionPopup, createCompletionPopup, destroyCompletionPopup } from './completion-popup';
import { canInsertNode } from '../../api/node';
import { setTextSelection } from 'prosemirror-utils';

export function completionExtension(handlers: readonly CompletionHandler[], events: EditorEvents) {
  return {
    plugins: () => [new CompletionPlugin(handlers, events)]
  };
}

interface CompletionState {
  handler?: CompletionHandler;
  result?: CompletionResult;
}


// TODO: keyboard and mouse navigation/selection
// TODO: insertion (may need to return arbitrary transactions for /command)

// TODO: invalidation token for multiple concurrent requests 
// (including cancel existing)

// TODO: built in caching and re-filtering?


// TODO: consider either getting rid of item height (instead just a max height for the flip)
// TODO: consider using em as the unit for height

// TODO: other solution is to use a fixed font size in pixels for completions
// TODO: do we need a fixed font size for the shelf


const key = new PluginKey<CompletionState>('completion');

class CompletionPlugin extends Plugin<CompletionState> {
  
  private readonly scrollUnsubscribe: VoidFunction;
  private readonly completionPopup: HTMLElement;

  // currently selected index and last set of completions are held as transient
  // state because they can't be derived from the document state (selectedIndex 
  // is derived from out of band user keyboard gestures and completions may 
  // have required fulfilling an external promise)
  private selectedIndex: number;
  private completions: any[];

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
          
          this.renderCompletions(view).then(this.showCompletions);
        
        },

        destroy: () => {

          // unsubscribe from events
          this.scrollUnsubscribe();
          window.document.removeEventListener('focusin', this.hideCompletions);

          // tear down the popup
          destroyCompletionPopup(this.completionPopup);

        },
      }),

      props: {
        handleDOMEvents: {
          keydown: (view: EditorView, event: Event) => {
            const kbEvent = event as KeyboardEvent;

            let handled = false;

            if (this.completionsActive()) {
              switch(kbEvent.key) {
                case 'Escape':
                  this.hideCompletions();
                  handled = true;
                  break;
                case 'Enter':
                  this.insertSelectedCompletion(view);
                  this.hideCompletions();
                  handled = true;
                  break;
                case 'ArrowUp':
                  this.selectedIndex--;
                  this.renderCompletions(view);
                  handled = true;
                  break;
                case 'ArrowDown':
                  this.selectedIndex++;
                  this.renderCompletions(view);
                  handled = true;
                  break;
              }
            }
            
            // supress event if we handled it
            if (handled) {
              event.preventDefault();
              event.stopPropagation();
            }

            // return status
            return handled;
          }
        }
      }
    });

    // initialize transient state
    this.selectedIndex = 0;
    this.completions = [];

    // bind callback methods
    this.showCompletions = this.showCompletions.bind(this);
    this.hideCompletions = this.hideCompletions.bind(this);
   
    // hide completions when we scroll or the focus changes
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.hideCompletions);
    window.document.addEventListener('focusin', this.hideCompletions);

    // create the popup, add it, and make it initially hidden
    this.completionPopup = createCompletionPopup();
    window.document.body.appendChild(this.completionPopup);
    this.hideCompletions();
  }

  private renderCompletions(view: EditorView) : Promise<boolean> {
    const state = key.getState(view.state);
    if (state?.handler) {

      // render using a helper so we can call the code from both sync and aysnc codepaths
      const render = (completions: any[]) : boolean => {

        // save completions as a side effect
        this.completions = completions;

        // render the popup if have completions
        if (completions.length > 0) {
          const props = {
            handler: state.handler!,
            pos: state.result!.pos,
            completions,
            selectedIndex: this.selectedIndex
          };
          renderCompletionPopup(view, props, this.completionPopup);
          return true;
        } else {
          return false;
        }
      };

      // resolve promise if needed
      if (state.result?.completions instanceof Promise) {
        return state.result?.completions.then(completions => {
          return render(completions);
        });
      } else {
        return Promise.resolve(render(state.result!.completions));
      }
    } else {
      return Promise.resolve(false);
    }
  }

  private insertSelectedCompletion(view: EditorView) {
    
    const state = key.getState(view.state);
    if (state?.handler) {

      // create transaction
      const tr = view.state.tr;
        
      // get replacement (provide marks if it's a text node)
      const result = state.result!;
      const replacement = state.handler.replacement(this.completions[this.selectedIndex]);
      const node = replacement instanceof ProsemirrorNode ? replacement : view.state.schema.text(replacement);

      // perform replacement
      tr.setSelection(new TextSelection(tr.doc.resolve(result.pos), view.state.selection.$head));
      tr.replaceSelectionWith(node, true);
      setTextSelection(tr.selection.to)(tr);
     
      // dispach
      view.dispatch(tr);

    }
  }

  private showCompletions(show: boolean) {
    this.completionPopup.style.display =  show ? '' : 'none';
  }

  private hideCompletions() {
    this.selectedIndex = 0;
    this.completions = [];
    this.showCompletions(false);
  }

  private completionsActive() {
    return this.completionPopup.style.display !== 'none';
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
