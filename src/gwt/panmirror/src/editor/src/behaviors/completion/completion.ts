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
import { MarkInputRuleFilter, markInputRuleFilter } from '../../api/input_rule';

export function completionExtension(
  handlers: readonly CompletionHandler[],
  inputRuleFilter: MarkInputRuleFilter,
  ui: EditorUI,
  events: EditorEvents) {
  return {
    plugins: () => [new CompletionPlugin(handlers, inputRuleFilter, ui, events)],
  };
}

interface CompletionState {
  handler?: CompletionHandler;
  result?: CompletionResult;
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
  private completions: any[] = [];
  private horizontal = false;
  private selectedIndex = 0;

  // events we need to unsubscribe from
  private readonly scrollUnsubscribe: VoidFunction;

  constructor(handlers: readonly CompletionHandler[], inputRuleFilter: MarkInputRuleFilter, ui: EditorUI, events: EditorEvents) {
    super({
      key,
      state: {
        init: () => ({}),
        apply: (tr: Transaction) => {
          // if we don't have a view then bail
          if (!this.view) {
            return {};
          }

          // selection only changes dismiss any active completion
          if (!tr.docChanged && tr.selectionSet) {
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

            // first check filter (null means apply no filter)
            if (handler.filter === null || (handler.filter ? handler.filter(tr) : inputRuleFilter(tr))) {

              // passted filter, check for completions
              const result = handler.completions(textBefore, tr);
              if (result) {
                return { handler, result };
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
          window.document.removeEventListener('focusin', this.hideCompletionPopup);

          // tear down the popup
          this.hideCompletionPopup();
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
    this.hideCompletionPopup = this.hideCompletionPopup.bind(this);
    this.scrollUnsubscribe = events.subscribe(ScrollEvent, this.hideCompletionPopup);
    window.document.addEventListener('focusin', this.hideCompletionPopup);
  }

  private updateCompletions(view: EditorView) {
    const state = key.getState(view.state);

    if (state?.handler) {
      // track the request version to invalidate the result if an
      // update happens after it goes into flight
      const requestVersion = this.version;

      // request completions
      return state.result!.completions(view.state).then(completions => {
        // if the version has incremented since the request then return false
        if (this.version !== requestVersion) {
          return false;
        }

        // save completions
        this.setCompletions(completions, state.handler?.view.horizontal);

        // render them
        this.renderCompletions(view);
      });
    } else {
      this.setCompletions([]);
      this.hideCompletionPopup();
    }
  }

  private renderCompletions(view: EditorView) {
    const state = key.getState(view.state);

    if (state && state.handler && (this.completions.length > 0 || !state.handler.view.hideNoResults)) {
      const props = {
        handler: state.handler!,
        pos: state.result!.pos,
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

          // ensure we have a node
          const node = replacement instanceof ProsemirrorNode ? replacement : view.state.schema.text(replacement);

          // combine it's marks w/ whatever is active at the selection
          const marks = view.state.selection.$head.marks();

          // set selection and replace it
          tr.setSelection(new TextSelection(tr.doc.resolve(result.pos), view.state.selection.$head));
          tr.replaceSelectionWith(node, false);

          // propapate marks
          marks.forEach(mark => tr.addMark(result.pos, view.state.selection.to, mark));

          // place cursor after the completion
          setTextSelection(tr.selection.to)(tr);

          // dispatch
          view.dispatch(tr);
        }
      }
      // set focus
      view.focus();
    }
    this.hideCompletionPopup();
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

    this.hideCompletionPopup();
  }

  private hideCompletionPopup() {
    this.setCompletions([]);
    if (this.completionPopup) {
      destroyCompletionPopup(this.completionPopup);
      this.completionPopup = null;
    }
  }

  private completionsActive() {
    return !!this.completionPopup;
  }

  private setCompletions(completions: any[], horizontal = false) {
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
