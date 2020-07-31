/*
 * list.ts
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
import { Transaction, Selection } from 'prosemirror-state';

import { findParentNodeOfType, setTextSelection } from 'prosemirror-utils';


export enum ListType {
  Ordered = 'OrderedList',
  Bullet = 'BulletList',
}

export interface ListCapabilities {
  tasks: boolean;
  fancy: boolean;
  example: boolean;
  order: boolean;
}

export function isList(node: ProsemirrorNode) {
  const schema = node.type.schema;
  return node.type === schema.nodes.bullet_list || node.type === schema.nodes.ordered_list;
}

export function precedingListItemInsertPos(doc: ProsemirrorNode, selection: Selection) {
  // selection just be empty
  if (!selection.empty) {
    return null;
  }

  // check for insert position in preceding list item
  const schema = doc.type.schema;
  const parentListItem = findParentNodeOfType(schema.nodes.list_item)(selection);
  if (parentListItem) {
    const $liPos = doc.resolve(parentListItem.pos);
    const listIndex = $liPos.index();
    if (listIndex > 0) {
      const pos = $liPos.pos - 1;
      return pos;
    } else {
      return null;
    }
  } else {
    return null;
  }
}

export function precedingListItemInsert(tr: Transaction, pos: number, node: ProsemirrorNode) {
  tr.deleteRange(tr.selection.from, tr.selection.from + 1);
  tr.insert(pos, node);
  setTextSelection(tr.mapping.map(pos), -1)(tr);
  return tr;
}
