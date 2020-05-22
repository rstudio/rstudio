/*
 * clear_formatting.ts
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

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';

import { Extension } from '../api/extension';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { liftTarget } from 'prosemirror-transform';

// marks included in clear formatting
const kFormattingMarks = ['code', 'em', 'smallcaps', 'span', 'strikeout', 'strong', 'superscript', 'subscript'];

// for nodes, all nodes with isTextblock === true will be converted to paragraph, and all
// nodes in this list will be lifted
const kLiftFormattingNodes = ['blockquote', 'line_block', 'div', 'raw_block'];

const extension: Extension = {
  commands: () => {
    return [new ProsemirrorCommand(EditorCommandId.ClearFormatting, ['Mod-\\'], clearFormatting)];
  },
};

export function clearFormatting(state: EditorState, dispatch?: (tr: Transaction) => void) {
  if (dispatch) {
    // create transaction
    const tr = state.tr;

    // alias schema and selection
    const schema = state.schema;
    const { from, to } = tr.selection;

    // clear formatting marks
    kFormattingMarks.forEach(markName => {
      const mark = state.schema.marks[markName];
      if (mark) {
        tr.removeMark(from, to, mark);
      }
    });
    tr.setStoredMarks([]);

    // lift / set nodes as required
    tr.doc.nodesBetween(from, to, (node: ProsemirrorNode, pos: number) => {
      // ignore paragraph and text nodes (already have 'cleared' formatting)
      if (node.type === schema.nodes.paragraph || node.type === schema.nodes.text) {
        return;
      }

      // pass recursively through list container nodes
      if (
        node.type === schema.nodes.bullet_list ||
        node.type === schema.nodes.ordered_list ||
        node.type === schema.nodes.definition_list ||
        node.type === schema.nodes.definition_list_term ||
        node.type === schema.nodes.definition_list_description
      ) {
        return;
      }

      // get node range (map positions)
      const fromPos = tr.doc.resolve(tr.mapping.map(pos + 1));
      const toPos = tr.doc.resolve(tr.mapping.map(pos + node.nodeSize - 1));
      const nodeRange = fromPos.blockRange(toPos);

      // process text blocks and blocks that can be lifted (e.g. blockquote)
      if (nodeRange) {
        if (node.type.isTextblock) {
          tr.setNodeMarkup(nodeRange.start, schema.nodes.paragraph);
        } else if (kLiftFormattingNodes.includes(node.type.name)) {
          const targetLiftDepth = liftTarget(nodeRange);
          if (targetLiftDepth || targetLiftDepth === 0) {
            tr.lift(nodeRange, targetLiftDepth);
          }
        }
      }
    });

    dispatch(tr);
  }

  return true;
}

export default extension;
