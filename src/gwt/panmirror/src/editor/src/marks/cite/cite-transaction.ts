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
