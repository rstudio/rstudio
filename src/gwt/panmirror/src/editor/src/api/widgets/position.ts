
/*
 * position.ts
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

import { EditorView } from "prosemirror-view";

export function popupPositionStylesForTextRange(
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
  const styles = { 
    top: '',
    left: '',
    bottom: '',
    right: ''  
  };

  // set them
  styles.top = Math.round(rangeEndCoords.bottom - editorBox.top) + 10 + 'px';
  styles.left = 'calc(' + Math.round(rangeStartCoords.left - editorBox.left) + 'px - 1ch)';
 
  // return the styles
  return styles;
}

export function elementHasPosition(el: HTMLElement, styles: { [key: string]: string }) {
  return el.style.left === styles.left &&
         el.style.top === styles.top &&
         el.style.right === styles.right &&
         el.style.bottom === styles.bottom;
}




