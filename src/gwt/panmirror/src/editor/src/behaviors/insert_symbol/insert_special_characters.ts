/*
 * insert_special_characters.ts
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

import { EditorState, Transaction } from "prosemirror-state";
import { EditorView } from "prosemirror-view";

import { ProsemirrorCommand, EditorCommandId } from "../../api/command";
import { canInsertNode } from "../../api/node";

const extension = {

  commands: () => {
    return [
      new InsertCharacterCommand(EditorCommandId.EmDash, '—', []),
      new InsertCharacterCommand(EditorCommandId.EnDash, '–', []),
      new InsertCharacterCommand(EditorCommandId.NonBreakingSpace, '\u00A0', ['Ctrl-Space', 'Ctrl-Shift-Space'])
    ];
  },
};

class InsertCharacterCommand extends ProsemirrorCommand {
  constructor(id: EditorCommandId, ch: string, keymap: string[]) {
    super(
      id,
      keymap,
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {

        // enable/disable command
        const schema = state.schema;
        if (!canInsertNode(state, schema.nodes.text)) {
          return false;
        }
        if (dispatch) {
          const tr = state.tr;
          tr.replaceSelectionWith(schema.text(ch), true).scrollIntoView();
          dispatch(tr);
        }

        return true;
      }
    );
  }
}


export default extension;
