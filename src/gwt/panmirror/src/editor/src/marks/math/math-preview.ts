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
import { getMarkRange } from "../../api/mark";
import { createPopup } from "../../api/widgets/widgets";
import { popupPositionStylesForTextRange } from "../../api/widgets/position";
import { EditorEvents, EditorEvent } from "../../api/events";
import { applyStyles } from "../../api/css";

const key = new PluginKey<DecorationSet>('math-preview');

export class MathPreviewPlugin extends Plugin<DecorationSet> {

  private readonly uiMath: EditorUIMath;

  private view: EditorView | null = null;

  private inlinePopup: HTMLElement | null = null;
  private lastRenderedInlineMath: string | null = null;
  
  private scrollUnsubscribe: VoidFunction;

  constructor(uiMath: EditorUIMath, events: EditorEvents) {
  
    super({
      key,
      view: () => {
        return {
          update: (view: EditorView) => {
            this.view = view;
            this.updateInlinePopup();
          },
          destroy: () => {
            this.scrollUnsubscribe();
            this.closeInlinePopup();
         
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

    // save reference to uiMath
    this.uiMath = uiMath;

    // update position on scroll
    this.updateInlinePopup = this.updateInlinePopup.bind(this);
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.updateInlinePopup);
  }

  private updateInlinePopup() {

    // bail if we don't have a view
    if (!this.view) {
      return;
    }

    // capture state, etc.
    const state = this.view.state;
    const schema = state.schema;
    const selection = state.selection;
     
    // are we in a math mark? if not bail
    const range = getMarkRange(selection.$from, schema.marks.math);
    if (!range) {
      this.closeInlinePopup();
      return;
    }

    // get the math text. bail if it's empty
    const inlineMath = state.doc.textBetween(range.from, range.to);
    if (inlineMath.match(/^\${1,2}\s*\${1,2}$/)) {
      this.closeInlinePopup();
      return;
    }    

    // get the position for the range
    const styles = popupPositionStylesForTextRange(this.view, range);

    // if the popup already exists just move it
    if (this.inlinePopup) {
      applyStyles(this.inlinePopup, [], styles);
    } else {
      this.inlinePopup = createPopup(this.view, ['pm-math-preview'], undefined, { 
        ...styles,
        visibility: 'hidden'
      });
      this.view.dom.parentNode?.appendChild(this.inlinePopup);
    }

    // typeset the math if we haven't already
    if (inlineMath !== this.lastRenderedInlineMath) {
      this.uiMath.typeset!(this.inlinePopup!, inlineMath).then(error => {
        if (!error) {
          this.inlinePopup!.style.visibility = 'visible';
          this.lastRenderedInlineMath = inlineMath; 
        }
      });
    }
  }

  private closeInlinePopup() {
    this.lastRenderedInlineMath = null;
    if (this.inlinePopup) {
      this.inlinePopup.remove();
      this.inlinePopup = null;
    }
  }
}
