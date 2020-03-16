/*
 * definition_list-commands.ts
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

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { Schema, Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { findParentNodeOfType, setTextSelection } from 'prosemirror-utils';

import { canInsertNode } from '../../api/node';
import { insertDefinitionList } from './definition_list-insert';

export class InsertDefinitionList extends ProsemirrorCommand {
  constructor() {
    super(
      EditorCommandId.DefinitionList,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        if (
          findParentNodeOfType(schema.nodes.definition_list)(state.selection) ||
          !canInsertNode(state, schema.nodes.definition_list)
        ) {
          return false;
        }

        // new definition list
        if (dispatch) {
          const tr = state.tr;

          // create new list
          insertDefinitionList(tr, [schema.nodes.definition_list_term.create()]);

          dispatch(tr);
        }

        return true;
      },
    );
  }
}

class InsertDefinitionListItemCommand extends ProsemirrorCommand {
  constructor(id: EditorCommandId, createFn: () => ProsemirrorNode) {
    super(id, [], (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
      const schema = state.schema;

      if (!findParentNodeOfType(schema.nodes.definition_list)(state.selection)) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;
        const dlTypes = [schema.nodes.definition_list_term, schema.nodes.definition_list_description];
        const parent = findParentNodeOfType(dlTypes)(state.selection)!;
        const insertPos = parent.pos + parent.node.nodeSize;
        tr.insert(insertPos, createFn());
        setTextSelection(insertPos, 1)(tr).scrollIntoView();
        dispatch(tr);
      }

      return true;
    });
  }
}

export class InsertDefinitionTerm extends InsertDefinitionListItemCommand {
  constructor(schema: Schema) {
    super(EditorCommandId.DefinitionTerm, () => schema.nodes.definition_list_term.create());
  }
}

export class InsertDefinitionDescription extends InsertDefinitionListItemCommand {
  constructor(schema: Schema) {
    super(EditorCommandId.DefinitionDescription, () => {
      return schema.nodes.definition_list_description.createAndFill({}, schema.nodes.paragraph.create())!;
    });
  }
}
