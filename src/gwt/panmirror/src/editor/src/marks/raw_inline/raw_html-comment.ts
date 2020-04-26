
/*
 * raw_html-comment.ts
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

import { Schema } from "prosemirror-model";
import { Transaction, TextSelection, EditorState } from "prosemirror-state";
import { toggleMark } from "prosemirror-commands";

import { setTextSelection } from "prosemirror-utils";

import { EditorCommandId, ProsemirrorCommand } from "../../api/command";

import { canInsertNode } from "../../api/node";


export class InsertHTMLCommentCommand extends ProsemirrorCommand {
  constructor(schema: Schema) {
    super(EditorCommandId.HTMLComment, ['Shift-Mod-c'], (state: EditorState, dispatch?: (tr: Transaction) => void) => {
      
      // make sure we can insert a text node here
      if (!canInsertNode(state, schema.nodes.text)) {
        return false;
      }

      // make sure we can apply this mark here
      if (!toggleMark(schema.marks.raw_html)(state)) {
        return false;
      }

      // make sure the end of the selection (where we will insert the comment) 
      // isn't already in a mark of this type
      if (state.doc.rangeHasMark(state.selection.to, state.selection.to+1, schema.marks.raw_html)) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;

        // set the selection to the end of the current selection (comment 'on' the selection)
        setTextSelection(tr.selection.to)(tr);

        // if we have a character right before us then insert a space
        const { parent, parentOffset } = tr.selection.$to;
        const charBefore = parent.textContent.slice(parentOffset - 1, parentOffset);
        if (charBefore.length && charBefore !== ' ') {
          tr.insertText(' ');
        }

        // insert the comment
        const mark = schema.marks.raw_html.create({ comment: true });
        const comment = '<!--  -->';
        const node = schema.text(comment, [mark]);
        tr.insert(tr.selection.from, node);

        // set the selection to the middle of the comment
        tr.setSelection(
          new TextSelection(tr.doc.resolve(tr.selection.from - (comment.length/2 - 1))),
        );

        // dispatch
        dispatch(tr);
      }

      return true;
    });
  }
}

