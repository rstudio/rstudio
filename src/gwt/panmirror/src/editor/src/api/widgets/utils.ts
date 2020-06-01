/*
 * utils.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

interface Point {
  x: number;
  y: number;
}

const kMinimumElementPadding = 5;

export function safePointForSelection(height: number, width: number, editorView: EditorView) : Point
{
  const selection = editorView.state.selection;
  const editorRect = editorView.dom.getBoundingClientRect();

  const selectionCoords = editorView.coordsAtPos(selection.from);

  const maximumTopPosition = Math.min(selectionCoords.bottom, window.innerHeight - height - kMinimumElementPadding);
  const minimumTopPosition = editorRect.y;
  const popupTopPosition = Math.max(minimumTopPosition, maximumTopPosition);

  const maximumLeftPosition = Math.min(selectionCoords.right, window.innerWidth - width - kMinimumElementPadding);
  const minimumLeftPosition = editorRect.x;
  const popupLeftPosition = Math.max(minimumLeftPosition, maximumLeftPosition);
  return {x: popupLeftPosition, y: popupTopPosition};
}

