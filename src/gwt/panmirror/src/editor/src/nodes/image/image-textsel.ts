/*
 * image-textsel.ts
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

import { EditorState, Transaction, NodeSelection } from 'prosemirror-state';
import { DecorationSet, Decoration } from 'prosemirror-view';
import { nodeDecoration } from '../../api/decoration';

export function imageTextSelectionInit(instance: EditorState) {
  return DecorationSet.empty;
}

export function imageTextSelectionApply(
  tr: Transaction,
  set: DecorationSet,
  oldState: EditorState,
  newState: EditorState,
) {
  // no decorations for empty or node selection
  if (tr.selection.empty || tr.selection instanceof NodeSelection) {
    return DecorationSet.empty;
  }

  const schema = newState.schema;
  const decorations: Decoration[] = [];
  tr.doc.nodesBetween(tr.selection.from, tr.selection.to, (node, pos) => {
    if ([schema.nodes.image, schema.nodes.figure].includes(node.type)) {
      decorations.push(nodeDecoration({ node, pos }, { class: 'pm-image-text-selection' }));
    }
  });

  return DecorationSet.create(tr.doc, decorations);
}
