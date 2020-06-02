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

import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { CompletionHandler } from '../../api/completion';
import { EditorEvents, EditorEvent } from '../../api/events';

import { renderCompletionPopup, createCompletionPopup, destroyCompletionPopup } from './completion-popup';

export function completionExtension(handlers: readonly CompletionHandler[], events: EditorEvents) {
  return {
    plugins: () => [new CompletionPlugin(handlers, events)]
  };
}

const key = new PluginKey('completion');

class CompletionPlugin extends Plugin {
  
  private readonly scrollUnsubscribe: VoidFunction;
  private readonly completionPopup: HTMLElement;

  constructor(handlers: readonly CompletionHandler[], events: EditorEvents) {
    super({
      key,
      view: () => ({
        update: (view: EditorView) => {
          // ask each handler if it has completions. if one does then render 
          // them and show the completion popup
          for (const handler of handlers) {
            const result = handler.completions(view.state, 20);
            if (result) {
              renderCompletionPopup(view, handler, result, this.completionPopup).then(() => {
                this.showCompletions();
              });
              return;
            }
          }

          // if we get this far there were no completions, so hide the popup
          this.hideCompletions();
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

    // hide completions when we scroll
    this.hideCompletions = this.hideCompletions.bind(this);
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.hideCompletions);

    // check for focus changes (e.g. dismiss when user clicks a menu)
    this.focusChanged = this.focusChanged.bind(this);
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

  private showCompletions() {
    this.completionPopup.style.display = '';
  }

  private hideCompletions() {
    this.completionPopup.style.display = 'none';
  }
}
