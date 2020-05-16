/*
 * table-paste.ts
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

import { Plugin, PluginKey, Transaction, EditorState } from 'prosemirror-state';
import { Slice, Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';

import { sliceHasNode } from '../../api/slice';

import { fixupTableWidths } from './table-columns';
import { forChangedNodes, trTransform } from '../../api/transaction';

import { kDefaultCellClasses } from './table-nodes';

export function tablePaste() {
  return new Plugin({
    key: new PluginKey('table-paste'),
    props: {
      handlePaste: (view: EditorView, _event: Event, slice: Slice) => {
        // if the slice contains a table then we handle it
        if (sliceHasNode(slice, node => node.type === node.type.schema.nodes.table)) {
          // based on https://github.com/ProseMirror/prosemirror-view/blob/fb799aae4e9dd5cfc256708a6845d76aaaf145bf/src/input.js#L503-L510
          const tr = view.state.tr.replaceSelection(slice);
          view.dispatch(
            tr
              .scrollIntoView()
              .setMeta('paste', true)
              .setMeta('tablePaste', true)
              .setMeta('uiEvent', 'paste')
          );
          view.dispatch(fixupTableWidths(view)(view.state.tr));
          return true;
        } else {
          return false;
        }
      },
    },
    appendTransaction: (transactions: Transaction[], oldState: EditorState, newState: EditorState) => {

      // alias schema
      const schema = newState.schema;

      // only process table paste transactions
      if (!transactions.some(transaction => transaction.getMeta('tablePaste'))) {
        return null;
      }

      // default to 'empty' attributes for cells
      const defaultCellAttrs = { colwidth: null, align: null, className: kDefaultCellClasses };
     
      // cleanup table by converting the first row to header cells and clearing any align styles
      // or other classes that may have come in
      const tr = newState.tr;
      forChangedNodes(oldState, newState, node => node.type === node.type.schema.nodes.table, (node, pos) => {
        let firstRow: ProsemirrorNode;
        node.descendants((childNode, childPos, parent) => {
          if (!firstRow) {
            firstRow = childNode;
          } else if (parent === firstRow) {
            const headerPos = pos + 1 + childPos;
            tr.setNodeMarkup(headerPos, schema.nodes.table_header, { ...childNode.attrs, ...defaultCellAttrs });
          } else if (childNode.type === schema.nodes.table_cell) {
            const cellPos = pos + 1 + childPos;
            tr.setNodeMarkup(cellPos, schema.nodes.table_cell, { ...childNode.attrs, ...defaultCellAttrs });
          }
        });
      });

      if (tr.docChanged) {
        return tr;
      }
    }
  });
}
