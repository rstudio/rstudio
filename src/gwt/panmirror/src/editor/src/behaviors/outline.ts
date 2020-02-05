/*
 * oultine.ts
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

import { Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';
import { Schema, Node as ProsemirrorNode } from 'prosemirror-model';
import { NodeWithPos } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { transactionsHaveChange } from '../api/transaction';
import { findTopLevelBodyNodes } from '../api/node';
import { uuidv4 } from '../api/util';
import { EditorOutlineItem, EditorOutlineItemType, EditorOutline } from '../api/outline';

const kOutlineIdsTransaction = 'OutlineIds';

const plugin = new PluginKey<EditorOutline>('outline');

const extension: Extension = {
  appendTransaction: (schema: Schema) => {
    return [
      {
        name: 'outline',
        filter: hasOutlineIdsTransaction,
        nodeFilter: isOutlineNode,
        append: (tr: Transaction) => {
          const usedIds = new Set<string>();
          const needsOutlineId = (node: ProsemirrorNode) => {
            if (isOutlineNode(node)) {
              if (node.attrs.navigation_id === null) {
                return true;
              } else if (usedIds.has(node.attrs.navigation_id)) {
                return true;
              } else {
                usedIds.add(node.attrs.navigation_id);
                return false;
              }
            } else {
              return false;
            }
          };
          const needsOutlineIdNodes = findTopLevelBodyNodes(tr.doc, needsOutlineId);
          needsOutlineIdNodes.forEach(needsOutlineId => {
            tr.setNodeMarkup(needsOutlineId.pos, needsOutlineId.node.type, {
              ...needsOutlineId.node.attrs,
              navigation_id: uuidv4(),
            });
            tr.setMeta(kOutlineIdsTransaction, true);
          });
        },
      },
    ];
  },

  plugins: (schema: Schema) => {
    return [
      new Plugin<EditorOutline>({
        key: plugin,

        // track editor outline as state changes
        state: {
          init: (_config, state: EditorState) => {
            return editorOutline(state);
          },
          apply: (tr: Transaction, value: EditorOutline, oldState: EditorState, newState: EditorState) => {
            if (transactionsAffectOutline([tr], oldState, newState)) {
              return editorOutline(newState);
            } else {
              return value;
            }
          },
        },
      }),
    ];
  },
};

function editorOutline(state: EditorState): EditorOutline {
  // get all of the headings (bail if there are none)
  const doc = state.doc;
  const outlineNodes = findTopLevelBodyNodes(doc, isOutlineNode);
  if (!outlineNodes.length) {
    return [];
  }

  // function to create an outline node from a heading
  const editorOutlineItem = (nodeWithPos: NodeWithPos, defaultLevel: number) => ({
    navigation_id: nodeWithPos.node.attrs.navigation_id,
    type: nodeWithPos.node.type.name as EditorOutlineItemType,
    level: nodeWithPos.node.attrs.level || defaultLevel,
    title: nodeWithPos.node.type.spec.code ? nodeWithPos.node.type.name : nodeWithPos.node.textContent,
    pos: nodeWithPos.pos,
    children: [],
  });

  // extract the outline
  const rootOutlineItem: EditorOutlineItem = {
    navigation_id: '',
    type: '' as EditorOutlineItemType,
    level: 0,
    title: '',
    pos: 0,
    children: [],
  };
  const containers: EditorOutlineItem[] = [];
  containers.push(rootOutlineItem);
  const top = () => containers[containers.length - 1];
  outlineNodes.forEach(nodeWithPos => {
    // get outline item from document node
    const outlineItem = editorOutlineItem(nodeWithPos, top().level + 1);

    // pop containers until we reach <= our level
    while (outlineItem.level <= top().level) {
      containers.pop();
    }

    // add the outline node
    top().children.push(outlineItem);

    // make it the the active container if it's a heading
    if (outlineItem.type === 'heading') {
      containers.push(outlineItem);
    }
  });

  // return children of root
  return rootOutlineItem.children;
}

function hasOutlineIdsTransaction(transactions: Transaction[]) {
  return transactions.some(tr => tr.getMeta(kOutlineIdsTransaction));
}

function isOutlineNode(node: ProsemirrorNode) {
  if (node.type.spec.attrs) {
    return node.type.spec.attrs.hasOwnProperty('navigation_id');
  } else {
    return false;
  }
}

function transactionsAffectOutline(transactions: Transaction[], oldState: EditorState, newState: EditorState) {
  return (
    hasOutlineIdsTransaction(transactions) || transactionsHaveChange(transactions, oldState, newState, isOutlineNode)
  );
}

export function getOutline(state: EditorState): EditorOutline {
  return plugin.getState(state);
}

export default extension;
