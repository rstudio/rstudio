
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
import { Transaction, TextSelection } from "prosemirror-state";

import { EditorCommandId } from "../../api/command";

import { RawInlineInsertCommand } from "./raw_inline";


export class InsertHTMLCommentCommand extends RawInlineInsertCommand {
  constructor(schema: Schema) {
    super(EditorCommandId.HTMLComment, ['Shift-Mod-c'], schema.marks.raw_html, (tr: Transaction) => {
      const mark = schema.marks.raw_html.create({ comment: true });
      const comment = '<!--  -->';
      const node = schema.text(comment, [mark]);
      tr.replaceSelectionWith(node, false);
      tr.setSelection(
        new TextSelection(tr.doc.resolve(tr.selection.from - (comment.length/2 - 1))),
      );
    });
  }
}

