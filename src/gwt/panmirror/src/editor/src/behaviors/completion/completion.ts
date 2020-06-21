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
import { Plugin, PluginKey, Transaction, Selection, TextSelection, EditorState } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { setTextSelection } from 'prosemirror-utils';

import {
  CompletionHandler,
  CompletionResult,
  selectionAllowsCompletions,
  kCompletionDefaultMaxVisible,
} from '../../api/completion';
import { EditorEvents } from '../../api/events';
import { ScrollEvent } from '../../api/event-types';

import { createCompletionPopup, renderCompletionPopup, destroyCompletionPopup } from './completion-popup';
import { EditorUI } from '../../api/ui';
import { PromiseQueue } from '../../api/promise';
import { MarkInputRuleFilter } from '../../api/input_rule';
import { kInsertCompletionTransaction } from '../../api/transaction';

export function completionExtension(
  handlers: readonly CompletionHandler[],
  inputRuleFilter: MarkInputRuleFilter,
  ui: EditorUI,
  events: EditorEvents,
) {
  return {
    plugins: () => [new CompletionPlugin(handlers, inputRuleFilter, ui, events)],
  };
}

interface CompletionState {
  handler?: CompletionHandler;
  result?: CompletionResult;
  prevToken?: string;
}

const key = new PluginKey<CompletionState>('completion');

class CompletionPlugin extends Plugin<CompletionState> {
  // editor ui
  private readonly ui: EditorUI;

  // editor view
  private view: EditorView | null = null;

  // popup elemeent
  private completionPopup: HTMLElement | null = null;

  // currently selected index and last set of completions are held as transient
  // state because they can't be derived from the document state (selectedIndex
  // is derived from out of band user keyboard gestures and completions may
  // have required fulfilling an external promise). also use a version counter
  // used to invalidate async completion requests that are fulfilled after
  // an update has occurred
  private version = 0;
  private allCompletions: any[] = [];
  private completions: any[] = [];
  private horizontal = false;
  private selectedIndex = 0;

  // serialize async completion requests
  private completionQueue = new PromiseQueue();

  // events we need to unsubscribe from
  private readonly scrollUnsubscribe: VoidFunction;

  constructor(
    handlers: readonly CompletionHandler[],
    inputRuleFilter: MarkInputRuleFilter,
    ui: EditorUI,
    events: EditorEvents,
  ) {
    super({
      key,
      state: {
        init: () => ({}),
        apply: (tr: Transaction, prevState: CompletionState) => {
          // if we don't have a view then bail
          if (!this.view) {
            return {};
          }

          // selection only changes dismiss any active completion
          if (!tr.docChanged && !tr.storedMarksSet && tr.selectionSet) {
            return {};
          }

          // check whether completions are valid here
          if (!selectionAllowsCompletions(tr.selection)) {
            return {};
          }

          // calcluate text before cursor
          const textBefore = completionTextBeforeCursor(tr.selection);

          // if there is no text then don't handle it
          if (textBefore.length === 0) {
            return {};
          }

          // check for a handler that can provide completions at the current selection
          for (const handler of handlers) {
            // first check if the handler is enabled (null means use inputRuleFilter)
            if (handler.enabled === null || (handler.enabled ? handler.enabled(tr) : inputRuleFilter(tr))) {
              const result = handler.completions(textBefore, tr);
              if (result) {
                // check if the previous state had a completion from the same handler
                let prevToken: string | undefined;
                if (handler.id === prevState.handler?.id) {
                  // suppress this handler if the last transaction was a completion result
                  if (tr.getMeta(kInsertCompletionTransaction)) {
                    continue;
                  }

                  // pass the prevToken on if the completion was for the same position
                  if (result.pos === prevState.result?.pos) {
                    prevToken = prevState.result.token;
                  }
                }

                // return state
                return { handler, result, prevToken };
              }
            }
          }

          // no handler found
          return {};
        },
      },

      view: () => ({
        update: (view: EditorView) => {
          // increment version
          this.version++;

          // set view
          this.view = view;

          // update completions
          this.updateCompletions(view);
        },

        destroy: () => {
          // unsubscribe from events
          this.scrollUnsubscribe();
          window.document.removeEventListener('focusin', this.clearCompletions);

          // tear down the popup
          this.clearCompletions();
        },
      }),

      props: {
        decorations: (state: EditorState) => {
          const pluginState = key.getState(state);
          return pluginState?.result?.decorations;
        },

        handleDOMEvents: {
          keydown: (view: EditorView, event: Event) => {
            const kbEvent = event as KeyboardEvent;

            let handled = false;

            // determine meaning of keys based on orientation
            const forwardKey = this.horizontal ? 'ArrowRight' : 'ArrowDown';
            const backwardKey = this.horizontal ? 'ArrowLeft' : 'ArrowUp';

            if (this.completionsActive()) {
              switch (kbEvent.key) {
                case 'Escape':
                  this.dismissCompletions();
                  handled = true;
                  break;
                case 'Enter':
                  this.insertCompletion(view, this.selectedIndex);
                  handled = true;
                  break;
                case backwardKey:
                  this.selectedIndex = Math.max(this.selectedIndex - 1, 0);
                  this.renderCompletions(view);
                  handled = true;
                  break;
                case forwardKey:
                  this.selectedIndex = Math.min(this.selectedIndex + 1, this.completions.length - 1);
                  this.renderCompletions(view);
                  handled = true;
                  break;
                case 'PageUp':
                  this.selectedIndex = Math.max(this.selectedIndex - this.completionPageSize(), 0);
                  this.renderCompletions(view);
                  handled = true;
                  break;
                case 'PageDown':
                  this.selectedIndex = Math.min(
                    this.selectedIndex + this.completionPageSize(),
                    this.completions.length - 1,
                  );
                  this.renderCompletions(view);
                  handled = true;
                  break;
                case 'End':
                  this.selectedIndex = this.completions.length - 1;
                  this.renderCompletions(view);
                  handled = true;
                  break;
                case 'Home':
                  this.selectedIndex = 0;
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
          },
        },
      },
    });

    // capture reference to ui
    this.ui = ui;

    // hide completions when we scroll or the focus changes
    this.clearCompletions = this.clearCompletions.bind(this);
    this.scrollUnsubscribe = events.subscribe(ScrollEvent, this.clearCompletions);
    window.document.addEventListener('focusin', this.clearCompletions);
  }

  private updateCompletions(view: EditorView) {
    const state = key.getState(view.state);

    if (state?.handler && state?.result) {
      // track the request version to invalidate the result if an
      // update happens after it goes into flight
      const requestVersion = this.version;

      // make an async request for the completions, update allCompletions,
      // and then apply any filter we have (allows the completer to just return
      // everything from the aysnc query and fall back to the filter for refinement)
      const requestAllCompletions = async () => {
        return state.result!.completions(view.state).then(completions => {
          // if we don't have a handler or result then return
          if (!state.handler || !state.result) {
            return;
          }

          // save completions
          this.setAllCompletions(completions, state.handler.view.horizontal);

          // display if the request still maps to the current state
          if (this.version === requestVersion) {
            // if there is a filter then call it and update displayed completions
            const displayedCompletions = state.handler.filter
              ? state.handler.filter(completions, view.state, state.result.token)
              : null;
            if (displayedCompletions) {
              this.setDisplayedCompletions(displayedCompletions, state.handler.view.horizontal);
            }

            this.renderCompletions(view);
          }
        });
      };

      // first see if we can do this exclusively via filter

      if (state.prevToken && state.handler.filter) {
        this.completionQueue.enqueue(
          () =>
            new Promise(resolve => {
              // display if the request still maps to the current state
              if (state.handler && state.result && this.version === requestVersion) {
                const filteredCompletions = state.handler.filter!(
                  this.allCompletions,
                  view.state,
                  state.result.token,
                  state.prevToken,
                );

                // got a hit from the filter!
                if (filteredCompletions) {
                  this.setDisplayedCompletions(filteredCompletions, state.handler.view.horizontal);
                  this.renderCompletions(view);

                  // couldn't use the filter, do a full request for all completions
                } else {
                  return this.completionQueue.enqueue(requestAllCompletions);
                }
              }

              resolve();
            }),
        );
      } else {
        // no prevToken or no filter for this handler, request everything
        this.completionQueue.enqueue(requestAllCompletions);
      }
    } else {
      // no handler/result for this document state
      this.clearCompletions();
    }
  }

  private renderCompletions(view: EditorView) {
    const state = key.getState(view.state);

    if (state && state.handler && (this.completions.length > 0 || !state.handler.view.hideNoResults)) {
      const props = {
        handler: state.handler!,
        pos: state.result!.pos + (state.result!.offset || 0),
        completions: this.completions,
        selectedIndex: this.selectedIndex,
        noResults: this.ui.context.translateText('No Results'),
        onClick: (index: number) => {
          this.insertCompletion(view, index);
        },
        onHover: (index: number) => {
          this.selectedIndex = index;
          this.renderCompletions(view);
        },
        ui: this.ui,
      };

      // create the completion popup if we need to
      if (this.completionPopup === null) {
        this.completionPopup = createCompletionPopup();
        window.document.body.appendChild(this.completionPopup);
      }

      // render
      renderCompletionPopup(view, props, this.completionPopup);
    } else {
      // hide
      this.hideCompletionPopup();
    }
  }

  private insertCompletion(view: EditorView, index: number) {
    // default index if not specified
    index = index || this.selectedIndex;

    const state = key.getState(view.state);
    if (state && state.handler) {
      // perform replacement
      const result = state.result!;

      // check low level handler first
      if (state.handler.replace) {
        // execute replace
        state.handler.replace(view, result.pos, this.completions[index]);

        // use higher level handler
      } else if (state.handler.replacement) {
        // get replacement from handler
        const replacement = state.handler.replacement(view.state.schema, this.completions[index]);
        if (replacement) {
          // create transaction
          const tr = view.state.tr;

          // set selection to area we will be replacing
          tr.setSelection(new TextSelection(tr.doc.resolve(result.pos), view.state.selection.$head));

          // ensure we have a node
          if (replacement instanceof ProsemirrorNode) {
            // combine it's marks w/ whatever is active at the selection
            const marks = view.state.selection.$head.marks();

            // set selection and replace it
            tr.replaceSelectionWith(replacement, false);

            // propapate marks
            marks.forEach(mark => tr.addMark(result.pos, view.state.selection.to, mark));
          } else {
            tr.insertText(replacement);
          }

          // mark the transaction as an completion insertin
          tr.setMeta(kInsertCompletionTransaction, true);

          // dispatch
          view.dispatch(tr);
        }
      }
      // set focus
      view.focus();
    }
    this.clearCompletions();
  }

  // explicit user dismiss of completion (e.g. Esc key)
  private dismissCompletions() {
    // call lower-level replace on any active handler (w/ null). this gives
    // them a chance to dismiss any artifacts that were explicitly inserted
    // to trigger the handler (e.g. a cmd+/ for omni-insert)
    if (this.view) {
      const state = key.getState(this.view.state);
      if (state?.result && state.handler) {
        if (state.handler.replace) {
          state.handler.replace(this.view, state.result.pos, null);
        } else if (state.handler.replacement) {
          state.handler.replacement(this.view.state.schema, null);
        }
      }
    }

    this.clearCompletions();
  }

  private clearCompletions() {
    this.setAllCompletions([]);
    this.hideCompletionPopup();
  }

  private hideCompletionPopup() {
    if (this.completionPopup) {
      destroyCompletionPopup(this.completionPopup);
      this.completionPopup = null;
    }
  }

  private completionsActive() {
    return !!this.completionPopup;
  }

  private setAllCompletions(completions: any[], horizontal = false) {
    this.allCompletions = completions;
    this.setDisplayedCompletions(completions, horizontal);
  }

  private setDisplayedCompletions(completions: any[], horizontal = false) {
    this.completions = completions;
    this.horizontal = !!horizontal;
    this.selectedIndex = 0;
  }

  private completionPageSize() {
    if (this.view) {
      const state = key.getState(this.view.state);
      return state?.handler?.view.maxVisible || kCompletionDefaultMaxVisible;
    } else {
      return kCompletionDefaultMaxVisible;
    }
  }
}

// extract the text before the cursor, dealing with block separators and
// non-text leaf chracters (this is based on code in prosemirror-inputrules)
function completionTextBeforeCursor(selection: Selection, maxLength = 500) {
  const { $head } = selection;
  return $head.parent.textBetween(
    Math.max(0, $head.parentOffset - maxLength), // start
    $head.parentOffset, // end
    undefined, // block separator
    '\ufffc', // leaf char
  );
}
