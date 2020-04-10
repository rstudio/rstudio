
/*
 * raw.ts
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
import { Schema } from "prosemirror-model";
import { EditorView } from "prosemirror-view";

import { findParentNodeOfType, setTextSelection } from "prosemirror-utils";

import { canInsertNode } from "./node";
import { EditorUI, RawFormatProps } from "./ui";

export const kTexFormat = 'tex';
export const kHTMLFormat = 'html';

export function editRawBlockCommand(ui: EditorUI) {
  
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
    const schema = state.schema;

    // enable if we are either inside a raw block or we can insert a raw block
    const rawBlock = findParentNodeOfType(schema.nodes.raw_block)(state.selection);
    if (!rawBlock && !canInsertNode(state, schema.nodes.raw_block)) {
      return false;
    }

    async function asyncEditRawBlock() {
      if (dispatch) {
        // get existing attributes (if any)
        const raw = {
          format: '',
          content: '',
        };
        if (rawBlock) {
          raw.format = rawBlock.node.attrs.format;
          raw.content = state.doc.textBetween(rawBlock.pos, rawBlock.pos + rawBlock.node.nodeSize);
        }

        // show dialog
        const result = await ui.dialogs.editRawBlock(raw);
        if (result) {
          const tr = state.tr;

          // remove means convert the block to text
          if (rawBlock) {
            const range = { from: rawBlock.pos, to: rawBlock.pos + rawBlock.node.nodeSize };
            if (result.action === 'remove') {
              tr.setBlockType(range.from, range.to, schema.nodes.paragraph);
            } else if (result.action === 'edit') {
              tr.replaceRangeWith(range.from, range.to, createRawNode(schema, result.raw));
              setTextSelection(tr.selection.from - 1, -1)(tr);
            }
          } else {
            insertRawNode(tr, result.raw);
          }

          dispatch(tr);
        }
      }

      if (view) {
        view.focus();
      }
    }
    asyncEditRawBlock();

    return true;
  };
}


// function to create a raw node
function createRawNode(schema: Schema, raw: RawFormatProps) {
  const rawText = raw.content ? schema.text(raw.content) : undefined;
  return schema.nodes.raw_block.createAndFill({ format: raw.format }, rawText)!;
}

// function to create and insert a raw node, then set selection inside of it
function insertRawNode(tr: Transaction, raw: RawFormatProps) {
  const schema = tr.doc.type.schema;
  const prevSel = tr.selection;
  tr.replaceSelectionWith(createRawNode(schema, raw));
  setTextSelection(tr.mapping.map(prevSel.from), -1)(tr);
}
