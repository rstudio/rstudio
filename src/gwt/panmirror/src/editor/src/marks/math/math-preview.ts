/*
 * math-preview.ts
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

import { Plugin, PluginKey, Transaction, EditorState } from "prosemirror-state";
import { DecorationSet, EditorView } from "prosemirror-view";

import { EditorUIMath } from "../../api/ui";
import { getMarkRange, getMarkAttrs } from "../../api/mark";
import { createPopup } from "../../api/widgets/widgets";
import { popupPositionStylesForTextRange, elementHasPosition } from "../../api/widgets/position";
import { EditorEvents, EditorEvent } from "../../api/events";

import { MathType } from "./math";

const key = new PluginKey<DecorationSet>('math-preview');

export class MathPreviewPlugin extends Plugin<DecorationSet> {

  private inlinePopup: HTMLElement | null = null;
  private lastRenderedInlineMath: string | null = null;
  private scrollUnsubscribe: VoidFunction;

  constructor(uiMath: EditorUIMath, events: EditorEvents) {
  
    super({
      key,
      view: () => {
        return {
          update: (view: EditorView, prevState: EditorState) => {

            // capture state, etc.
            const state = view.state;
            const schema = state.schema;
            const selection = state.selection;
            
            // are we in a math mark? if not bail
            const range = getMarkRange(selection.$from, schema.marks.math);
            if (!range) {
              this.closeInlinePopup();
              return;
            }
            // is this inline math? if not bail
            const attrs = getMarkAttrs(state.doc, range, schema.marks.math);
            if (attrs.type !== MathType.Inline) {
              this.closeInlinePopup();
              return;
            }

            // get the math text. bail if it's empty
            const inlineMath = state.doc.textBetween(range.from, range.to);
            if (inlineMath.match(/^\$\s*\$$/)) {
              this.closeInlinePopup();
              return;
            }    

            // get the position for the range
            const styles = popupPositionStylesForTextRange(view, range);

            // if we don't have a poupup for this position then close the popup
            if (this.inlinePopup && !elementHasPosition(this.inlinePopup, styles)) {
              this.closeInlinePopup();
            }

            // create the popup if we need to
            if (!this.inlinePopup) {
              this.inlinePopup = createPopup(view, ['pm-math-preview'], undefined, styles);
              window.document.body.appendChild(this.inlinePopup); 
            }

            // typeset the math if we haven't already
            if (inlineMath !== this.lastRenderedInlineMath) {
              uiMath.typeset!(this.inlinePopup!, inlineMath).then(error => {
                if (!error) {
                  this.lastRenderedInlineMath = inlineMath;
                }
              });
            }
          },
          destroy: () => {
            this.closeInlinePopup();
            this.scrollUnsubscribe();
          }
        };
      },
      state: {
        init: (_config: { [key: string]: any }) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          return DecorationSet.empty;         
        },
      },
      props: {
        decorations: (state: EditorState) => {
          return key.getState(state);
        },
      },
    });

    // subscribe to scroll events (and close popup)
    this.closeInlinePopup = this.closeInlinePopup.bind(this);
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.closeInlinePopup);
  }

  private closeInlinePopup() {
    this.lastRenderedInlineMath = null;
    if (this.inlinePopup) {
      this.inlinePopup.remove();
      this.inlinePopup = null;
    }
  }
}