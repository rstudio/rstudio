/*
 * figure-keys.ts
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

import { Schema } from "prosemirror-model";
import { EditorState, Transaction, NodeSelection, Selection } from "prosemirror-state";

import { BaseKey } from "../../api/basekeys";
import { exitNode } from "../../api/command";
import { EditorView } from "prosemirror-view";
import { findParentNodeOfTypeClosestToPos } from "prosemirror-utils";


export function figureKeys(schema: Schema) {
  return [
    { key: BaseKey.Enter, command: exitNode(schema.nodes.figure, -1, false) },
    { key: BaseKey.Backspace, command: backspaceHandler() },
    { key: BaseKey.ArrowLeft, command: arrowHandler('left') },
    { key: BaseKey.ArrowRight, command: arrowHandler('right') },
    { key: BaseKey.ArrowUp, command: arrowHandler('up') },
    { key: BaseKey.ArrowDown, command: arrowHandler('down') },
  ];
}


function backspaceHandler() {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {

    // must be an empty selection
    const selection = state.selection;
    if (!selection.empty) {
      return false;
    }

    // must be a selection at the beginning of it's parent
    const schema = state.schema;
    const { $head } = state.selection;
    const { parentOffset } = $head;
    if (parentOffset !== 0) {
      return false;
    }

    // two scenarios: backspace within empty caption or backspace right after figure
    const isWithinEmptyCaption = $head.parent.type === schema.nodes.figure && 
                                 $head.parent.childCount === 0;
    if (isWithinEmptyCaption) {
      if (dispatch) {
        // set a node selection for the figure
        const tr = state.tr;
        tr.setSelection(NodeSelection.create(tr.doc, $head.pos - 1));
        dispatch(tr);
      }
      return true;
    } else {
       // check if the previous node is a figure
      const parent = $head.node($head.depth - 1);
      const parentIndex = $head.index($head.depth - 1);
      if (parentIndex > 0) {
        const previousNode = parent.child(parentIndex - 1);
        if (previousNode.type === schema.nodes.figure) {
          if (dispatch) {
            const tr = state.tr;
            const nodePos = selection.head - previousNode.nodeSize - 1;
            const figureSelection = NodeSelection.create(state.doc, nodePos);
            tr.setSelection(figureSelection);
            dispatch(tr);
          }
          return true;
        }
      }
    }

    return false;
  };
}


function arrowHandler(dir: 'up' | 'down' | 'left' | 'right' | 'forward' | 'backward') {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {

    const schema = state.schema;

    if (state.selection.empty && view && view.endOfTextblock(dir)) {
      
      // compute side offset
      const side = dir === 'left' || dir === 'up' ? -1 : 1;

      // get selection head
      const selection = state.selection;
      const { $head } = selection;

      // see if this would traverse our type
      const nextPos = Selection.near(state.doc.resolve(side > 0 ? $head.after() : $head.before()), side);
      if (nextPos.$head) {
        const figure = findParentNodeOfTypeClosestToPos(nextPos.$head, schema.nodes.figure);
        if (figure && figure.node.textContent.length === 0) {
          if (dispatch) {
            const tr = state.tr;
            const figureSelection = NodeSelection.create(state.doc, figure.pos);
            tr.setSelection(figureSelection);
            dispatch(tr);
          }
          return true;
        }
      }
    }
    return false;
  };
}

 