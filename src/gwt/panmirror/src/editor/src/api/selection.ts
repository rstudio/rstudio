/*
 * selection.ts
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

import { Selection } from 'prosemirror-state';
import { NodeWithPos } from 'prosemirror-utils';

export function selectionIsWithin(selection: Selection, nodeWithPos: NodeWithPos) {
  const begin = nodeWithPos.pos + 1;
  const end = begin + nodeWithPos.node.nodeSize;
  return selection.anchor >= begin && selection.anchor <= end;
}

export function selectionIsBodyTopLevel(selection: Selection) {
  const { $head } = selection;
  const parentNode = $head.node($head.depth - 1);
  return parentNode && parentNode.type === parentNode.type.schema.nodes.body;
}
