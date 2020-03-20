/*
 * list-commands.ts
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

import { NodeType, Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { findParentNodeOfType, NodeWithPos } from 'prosemirror-utils';

import { NodeCommand, toggleList, ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { EditorUI, OrderedListProps } from '../../api/ui';
import { ListCapabilities } from '../../api/list';

export class ListCommand extends NodeCommand {
  constructor(id: EditorCommandId, keymap: string[], listType: NodeType, listItemType: NodeType) {
    super(id, keymap, listType, {}, toggleList(listType, listItemType));
  }
}

export class TightListCommand extends ProsemirrorCommand {
  constructor() {
    super(
      EditorCommandId.TightList,
      ['Shift-Ctrl-9'],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;
        const listTypes = [schema.nodes.bullet_list, schema.nodes.ordered_list];
        const parentList = findParentNodeOfType(listTypes)(state.selection);
        if (!parentList) {
          return false;
        }

        if (dispatch) {
          const tr = state.tr;
          const node = parentList.node;
          tr.setNodeMarkup(parentList.pos, node.type, {
            ...node.attrs,
            tight: !node.attrs.tight,
          });
          dispatch(tr);
        }

        return true;
      },
    );
  }

  public isActive(state: EditorState): boolean {
    if (this.isEnabled(state)) {
      const listTypes = [state.schema.nodes.bullet_list, state.schema.nodes.ordered_list];
      const itemNode = findParentNodeOfType(listTypes)(state.selection) as NodeWithPos;
      return itemNode.node.attrs.tight;
    } else {
      return false;
    }
  }
}

export class OrderedListEditCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI, capabilities: ListCapabilities) {
    super(
      EditorCommandId.OrderedListEdit,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        // see if a parent node is an ordered list
        const schema = state.schema;
        let node: ProsemirrorNode | null = null;
        let pos: number = 0;
        const nodeWithPos = findParentNodeOfType(schema.nodes.ordered_list)(state.selection);
        if (nodeWithPos) {
          node = nodeWithPos.node;
          pos = nodeWithPos.pos;
        }

        // return false (disabled) for no targets
        if (!node) {
          return false;
        }

        // execute command when requested
        async function asyncEditList() {
          if (dispatch) {
            await editOrderedList(node as ProsemirrorNode, pos, state, dispatch, ui, capabilities);
            if (view) {
              view.focus();
            }
          }
        }
        asyncEditList();

        return true;
      },
    );
  }
}

async function editOrderedList(
  node: ProsemirrorNode,
  pos: number,
  state: EditorState,
  dispatch: (tr: Transaction<any>) => void,
  ui: EditorUI,
  capabilities: ListCapabilities,
): Promise<void> {
  const attrs = node.attrs;
  const result = await ui.dialogs.editOrderedList({ ...attrs } as OrderedListProps, capabilities);
  if (result) {
    const tr = state.tr;
    tr.setNodeMarkup(pos, node.type, {
      ...attrs,
      ...result,
    });
    dispatch(tr);
  }
}
