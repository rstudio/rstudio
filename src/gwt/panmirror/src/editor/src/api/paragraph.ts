/*
 * paragraph.ts
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

import { EditorState, Transaction } from 'prosemirror-state';

import { setTextSelection } from 'prosemirror-utils';

import { canInsertNode } from './node';

export function insertParagraph(state: EditorState, dispatch?: (tr: Transaction) => void) {
  const schema = state.schema;

  if (!canInsertNode(state, schema.nodes.paragraph)) {
    return false;
  }

  if (dispatch) {
    const tr = state.tr;
    tr.replaceSelectionWith(schema.nodes.paragraph.create());
    setTextSelection(state.selection.from + 1, 1)(tr);
    dispatch(tr);
  }

  return true;
}
