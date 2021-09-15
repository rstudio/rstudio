/*
 * div.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import { Transaction, EditorState } from "prosemirror-state";
import { wrapIn } from "prosemirror-commands";
import { ContentNodeWithPos, findParentNodeOfType } from "prosemirror-utils";

import { EditorUI } from "./ui";
import { DivEditProps, DivEditResult } from "./ui-dialogs";

export async function createDiv(ui: EditorUI, state: EditorState, dispatch: (tr: Transaction) => void, 
                                props?: DivEditProps,
                                insertFn?: (result: DivEditResult, tr: Transaction, div: ContentNodeWithPos) => void) {
  props = props || { attr: {} };
  const result = await ui.dialogs.editDiv(props, false);
  if (result) {
    wrapIn(state.schema.nodes.div)(state, (tr: Transaction) => {
      const div = findParentNodeOfType(state.schema.nodes.div)(tr.selection)!;
      tr.setNodeMarkup(div.pos, div.node.type, result.attr);   
      if (insertFn) {
        insertFn(result, tr, div);
      } 
      dispatch(tr);
    });
  }
}

