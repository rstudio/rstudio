/*
 * remove_section.ts
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

import { Schema } from 'prosemirror-model';
import { Transaction, EditorState, Selection } from 'prosemirror-state';
import { ReplaceStep, ReplaceAroundStep } from 'prosemirror-transform';

import { Extension } from '../api/extension';

const extension: Extension = {
  appendTransaction: (schema: Schema) => {
    return [
      {
        name: 'remove_section',
        append: (tr: Transaction, transactions: Transaction[], oldState: EditorState, _newState: EditorState) => {
          // if we are left with an empty selection in an empty heading block this may 
          // have been the removal of a section (more than 1 textBlock). in that case
          // remove the empty heading node
          if (isEmptyHeadingSelection(tr.selection) && isSectionRemoval(transactions, oldState)) {
            const $head = tr.selection.$head;
            const start = $head.start();
            const end = start + 2;
            tr.deleteRange(start, end);
          }
        },
      },
    ];
  },
};

function isEmptyHeadingSelection(selection: Selection) {
  const parent = selection.$head.parent;
  const schema = parent.type.schema;
  return selection.empty && parent.type === schema.nodes.heading && parent.content.size === 0;
}

function isSectionRemoval(transactions: Transaction[], state: EditorState) {
  // was this the removal of a section?
  let isRemoval = false;
  if (transactions.length === 1 && transactions[0].steps.length === 1) {
    // see if this is a delete step
    let isDeleteStep = false;
    const step = transactions[0].steps[0];
    if (step instanceof ReplaceStep) {
      isDeleteStep = (step as any).slice.content.size === 0;
    } else if (step instanceof ReplaceAroundStep) {
      const { gapFrom, gapTo } = step as any;
      isDeleteStep = gapFrom === gapTo;
    }

    // if it's a delete step then see if we removed multiple text blocks
    let numBlocks = 0;
    const { from, to } = step as any;
    state.doc.nodesBetween(from, to, node => {
      if (isRemoval) {
        return false;
      }
      if (node.isTextblock) {
        if (numBlocks++ >= 1) {
          isRemoval = true;
          return false;
        }
      }
    });
  }

  return isRemoval;

}

export default extension;
