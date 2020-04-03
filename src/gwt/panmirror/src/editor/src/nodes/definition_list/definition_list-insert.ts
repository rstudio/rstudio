/*
 * definition_list-insert.ts
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
import { Transaction } from 'prosemirror-state';
import { setTextSelection } from 'prosemirror-utils';
import { findChildrenByType } from 'prosemirror-utils';

export function insertDefinitionList(tr: Transaction, items: ProsemirrorNode[]) {
  const schema = items[0].type.schema;
  const definitionList = schema.nodes.definition_list.createAndFill({}, items)!;
  const prevCursor = tr.selection.to;
  tr.replaceSelectionWith(definitionList);
  setTextSelection(tr.mapping.map(prevCursor) - 1, -1)(tr).scrollIntoView();
  return tr;
}

export function insertDefinitionListAppendTransaction() {
  return {
    name: 'definition-list-join',
    nodeFilter: (node: ProsemirrorNode) => node.type === node.type.schema.nodes.definition_list,
    append: (tr: Transaction) => {
      // if a transaction creates 2 adjacent definition lists then join them
      const schema = tr.doc.type.schema;
      const lists = findChildrenByType(tr.doc, schema.nodes.definition_list, true);
      for (const list of lists) {
        const listPos = tr.doc.resolve(list.pos + 1);
        const listIndex = listPos.index(listPos.depth - 1);
        const listParent = listPos.node(listPos.depth - 1);
        if (listIndex + 1 < listParent.childCount) {
          const nextNode = listParent.child(listIndex + 1);
          if (nextNode.type === schema.nodes.definition_list) {
            tr.join(list.pos + list.node.nodeSize);
            return;
          }
        }
      }
    },
  };
}
