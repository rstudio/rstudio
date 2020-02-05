/*
 * cite-transaction.ts
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

import { Node as ProsemirrorNode } from 'prosemirror-model';

import { AppendMarkTransactionHandler, MarkTransaction } from '../../api/transaction';
import { detectAndApplyMarks, removeInvalidatedMarks } from '../../api/mark';

const kCiteRe = /\[(.* -?@|-?@)[\w:.#$%&-+?<>~/]+.*\]/;

export function citeAppendMarkTransaction(): AppendMarkTransactionHandler {
  return {
    name: 'cite-marks',

    filter: node => node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.cite),

    append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
      // find citation marks where the text is not a citation (remove the mark)
      removeInvalidatedMarks(tr, node, pos, kCiteRe, node.type.schema.marks.cite);

      // find citations that aren't marked (add the mark)
      detectAndApplyMarks(tr, tr.doc.nodeAt(pos)!, pos, kCiteRe, node.type.schema.marks.cite);
    },
  };
}
