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

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { Plugin, PluginKey, Transaction, Selection, TextSelection,} from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { setTextSelection } from 'prosemirror-utils';

import { CompletionHandler, CompletionResult } from '../../api/completion';
import { EditorEvents } from '../../api/events';
import { ScrollEvent } from '../../api/event-types';

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

const key = new PluginKey<CompletionState>('completion');

class CompletionPlugin extends Plugin<CompletionState> {
  
  private readonly scrollUnsubscribe: VoidFunction;
  private readonly completionPopup: HTMLElement;

  // currently selected index and last set of completions are held as transient
  // state because they can't be derived from the document state (selectedIndex 
  // is derived from out of band user keyboard gestures and completions may 
  // have required fulfilling an external promise). also use a version counter
  // used to invalidate async completion requests that are fulfilled after
  // an update has occurred
  private version = 0;
  private completions: any[] = [];
  private selectedIndex = 0;

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
          
          // clear completion state
          this.completions = [];
          this.selectedIndex = 0;

          // increment version
          this.version++;

          // render and show completions if we have them
          this.updateCompletions(view).then(this.showCompletions);
        
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
                  this.insertCompletion(view, this.selectedIndex);
                  this.hideCompletions();
                  handled = true;
                  break;
                case 'ArrowUp':
                  this.selectedIndex = Math.max(this.selectedIndex - 1, 0);
                  this.renderCompletions(view);
                  handled = true;
                  break;
                case 'ArrowDown':
                  this.selectedIndex = Math.min(this.selectedIndex + 1, this.completions.length - 1);
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

    // bind callback methods
    this.showCompletions = this.showCompletions.bind(this);
    this.hideCompletions = this.hideCompletions.bind(this);
   
    // hide completions when we scroll or the focus changes
    this.scrollUnsubscribe = events.subscribe(ScrollEvent, this.hideCompletions);
    window.document.addEventListener('focusin', this.hideCompletions);

    // create the popup, add it, and make it initially hidden
    this.completionPopup = createCompletionPopup();
    window.document.body.appendChild(this.completionPopup);
    this.hideCompletions();
  }

  private updateCompletions(view: EditorView) : Promise<boolean> {

    const state = key.getState(view.state);
    
    if (state?.handler) {
  
      // track the request version to invalidate the result if an
      // update happens after it goes into flight
      const requestVersion = this.version;

      // request completions
      return state.result!.completions.then(completions => {

        // if the version has incremented since the request then return false
        if (this.version !== requestVersion) {
          return false;
        }
         
        // save completions 
        this.completions = completions;

        // render them
        return this.renderCompletions(view);

      });
     
    } else {
      return Promise.resolve(false);
    }
  }

  private renderCompletions(view: EditorView) {

    const state = key.getState(view.state);

    if (state?.handler && this.completions.length > 0) {
      const props = {
        handler: state.handler!,
        pos: state.result!.pos,
        completions: this.completions,
        selectedIndex: this.selectedIndex,
        onClick: (index: number) => {
          this.insertCompletion(view, index);
          this.hideCompletions();
        },
        onHover: (index: number) => {
          this.selectedIndex = index;
          this.renderCompletions(view);
        }
      };
      renderCompletionPopup(view, props, this.completionPopup);
      return true;
    } else {
      return false;
    }
  }

  private insertCompletion(view: EditorView, index: number) {

    // default index if not specified
    index = index || this.selectedIndex;

    const state = key.getState(view.state);
    if (state?.handler) {

      // create transaction
      const tr = view.state.tr;
        
      // get replacement 
      const result = state.result!;
      const replacement = state.handler.replacement(view.state.schema, this.completions[index]);
      const node = replacement instanceof ProsemirrorNode ? replacement : view.state.schema.text(replacement);

      // perform replacement
      tr.setSelection(new TextSelection(tr.doc.resolve(result.pos), view.state.selection.$head));
      tr.replaceSelectionWith(node, true);
      setTextSelection(tr.selection.to)(tr);
     
      // dispach
      view.dispatch(tr);
      view.focus();

    }
  }

  private showCompletions(show: boolean) {
    this.completionPopup.style.display =  show ? '' : 'none';
  }

  private hideCompletions() {
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
