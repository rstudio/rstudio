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

import { Plugin, PluginKey } from "prosemirror-state";
import { EditorView } from "prosemirror-view";

import debounce from 'lodash.debounce';

import { EditorUIMath } from "../../api/ui";
import { getMarkRange } from "../../api/mark";
import { createPopup } from "../../api/widgets/widgets";
import { EditorEvents, EditorEvent } from "../../api/events";
import { applyStyles } from "../../api/css";
import { ResolvedPos } from "prosemirror-model";

const key = new PluginKey('math-preview');

export class MathPreviewPlugin extends Plugin {

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
      props: {
        handleDOMEvents: {
          mousemove: debounce((view: EditorView, event: Event) => {
            const ev = event as MouseEvent;
            const pos = view.posAtCoords({ top: ev.clientY, left: ev.clientX });
            if (pos && pos.inside !== -1) {
              this.updateInlinePopup(view.state.doc.resolve(pos.pos));
            }
            return false;
          }, 250),
        },
      },
    });

    // save reference to uiMath
    this.uiMath = uiMath;

    // update position on scroll
    this.updateInlinePopup = this.updateInlinePopup.bind(this);
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, () => this.updateInlinePopup());
  }

  private updateInlinePopup($pos?: ResolvedPos) {

    // bail if we don't have a view
    if (!this.view) {
      return;
    }

    // capture state, etc.
    const state = this.view.state;
    const schema = state.schema;

    // determine math range
    let range: false | { from: number, to: number } = false;

    // if a $pos was passed (e.g. for a mouse hover) then check that first
    if ($pos) {
      range = getMarkRange($pos, schema.marks.math);
    }

    // if that didn't work try the selection
    if (!range) {
      range = getMarkRange(state.selection.$from, schema.marks.math)
    }

    // bail if we don't have a target
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
    const styles = mathjaxPopupPositionStyles(this.view, range);

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



function mathjaxPopupPositionStyles(
  view: EditorView, 
  range: { from: number, to: number }
) {

  // get coordinates for editor view (use to offset)
  const editorBox = (view.dom.parentNode! as HTMLElement).getBoundingClientRect();
 
  // +1 to ensure beginning of line doesn't resolve as line before
  // (will subtract it back out below)
  const rangeStartCoords = view.coordsAtPos(range.from + 1); 
  const rangeEndCoords = view.coordsAtPos(range.to);

  // styles we'll return
  return { 
    top: Math.round(rangeEndCoords.bottom - editorBox.top) + 10 + 'px',
    left: 'calc(' + Math.round(rangeStartCoords.left - editorBox.left) + 'px - 1ch)',
  };
}


