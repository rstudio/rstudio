/*
 * table.ts
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

import { Schema } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';
import { Transaction } from 'prosemirror-state';

import { tableEditing, columnResizing, goToNextCell, deleteColumn, deleteRow } from 'prosemirror-tables';

import { findChildrenByType, setTextSelection } from 'prosemirror-utils';

import { EditorUI } from '../../api/ui';
import { Extension } from '../../api/extension';
import { PandocExtensions } from '../../api/pandoc';
import { BaseKey } from '../../api/basekeys';
import { ProsemirrorCommand, EditorCommandId, exitNode } from '../../api/command';

import {
  deleteTable,
  TableColumnAlignmentCommand,
  insertTable,
  TableToggleHeaderCommand,
  TableToggleCaptionCommand,
  CssAlignment,
  deleteTableCaption,
  addRows,
  addColumns,
} from './table-commands';

import {
  tableContainerNode,
  tableNode,
  tableCaptionNode,
  tableCellNode,
  tableHeaderNode,
  tableRowNode,
} from './table-nodes';

import { fixupTableWidths } from './table-columns';

import { tablePaste } from './table-paste';

import 'prosemirror-tables/style/tables.css';
import './table-styles.css';
import { TableCapabilities } from '../../api/table';

const extension = (pandocExtensions: PandocExtensions): Extension | null => {
  // not enabled if there are no tables enabled
  if (
    !pandocExtensions.grid_tables &&
    !pandocExtensions.pipe_tables &&
    !pandocExtensions.simple_tables &&
    !pandocExtensions.multiline_tables
  ) {
    return null;
  }

  // define table capabilities
  const capabilities: TableCapabilities = {
    captions: pandocExtensions.table_captions,
    headerOptional: pandocExtensions.grid_tables,
  };

  return {
    nodes: [
      tableContainerNode,
      tableNode,
      tableCaptionNode,
      tableCellNode(pandocExtensions.grid_tables),
      tableHeaderNode,
      tableRowNode,
    ],

    commands: (_schema: Schema, ui: EditorUI) => {
      const commands = [
        new ProsemirrorCommand(
          EditorCommandId.TableInsertTable,
          ['Alt-Mod-t'],
          insertTable(capabilities, ui.dialogs.insertTable),
        ),
        new ProsemirrorCommand(EditorCommandId.TableNextCell, ['Tab'], goToNextCell(1)),
        new ProsemirrorCommand(EditorCommandId.TablePreviousCell, ['Shift-Tab'], goToNextCell(-1)),
        new ProsemirrorCommand(EditorCommandId.TableAddColumnAfter, [], addColumns(true)),
        new ProsemirrorCommand(EditorCommandId.TableAddColumnBefore, [], addColumns(false)),
        new ProsemirrorCommand(EditorCommandId.TableDeleteColumn, [], deleteColumn),
        new ProsemirrorCommand(EditorCommandId.TableAddRowAfter, [], addRows(true)),
        new ProsemirrorCommand(EditorCommandId.TableAddRowBefore, [], addRows(false)),
        new ProsemirrorCommand(EditorCommandId.TableDeleteRow, [], deleteRow),
        new ProsemirrorCommand(EditorCommandId.TableDeleteTable, [], deleteTable()),
        new TableColumnAlignmentCommand(EditorCommandId.TableAlignColumnLeft, CssAlignment.Left),
        new TableColumnAlignmentCommand(EditorCommandId.TableAlignColumnRight, CssAlignment.Right),
        new TableColumnAlignmentCommand(EditorCommandId.TableAlignColumnCenter, CssAlignment.Center),
        new TableColumnAlignmentCommand(EditorCommandId.TableAlignColumnDefault, null),
      ];
      if (capabilities.captions) {
        commands.push(new TableToggleCaptionCommand());
      }
      if (capabilities.headerOptional) {
        commands.push(new TableToggleHeaderCommand());
      }
      return commands;
    },

    plugins: (_schema: Schema) => {
      return [
        columnResizing({
          handleWidth: 8,
        }),
        tableEditing(),
        tablePaste(),
      ];
    },

    baseKeys: (schema: Schema) => {
      return [
        { key: BaseKey.Backspace, command: deleteTableCaption() },
        { key: BaseKey.Enter, command: exitNode(schema.nodes.table_caption, -2, false) },
        { key: BaseKey.Tab, command: goToNextCell(1) },
        { key: BaseKey.ShiftTab, command: goToNextCell(-1) },
      ];
    },

    layoutFixups: (_schema: Schema, view: EditorView) => {
      return [fixupTableWidths(view)];
    },

    appendTransaction: (_schema: Schema) => {
      return [
        {
          name: 'table-repair',
          nodeFilter: node => node.type === node.type.schema.nodes.table,
          append: (tr: Transaction) => {
            const schema = tr.doc.type.schema;
            const tables = findChildrenByType(tr.doc, schema.nodes.table);
            tables.forEach(table => {
              // map the position
              const pos = tr.mapping.map(table.pos);

              // get containing node (pos is right before the table)
              const containingNode = tr.doc.resolve(pos).node();

              // table with no container
              if (containingNode.type !== schema.nodes.table_container) {
                // add the container
                const caption = schema.nodes.table_caption.createAndFill({ inactive: true }, undefined)!;
                const container = schema.nodes.table_container.createAndFill({}, [table.node, caption])!;
                tr.replaceWith(pos, pos + table.node.nodeSize, container);
              }

              // table with no content (possible w/ half caption leftover)
              if (table.node.firstChild && table.node.firstChild.childCount === 0) {
                // delete the table
                const hasContainer = containingNode.type === schema.nodes.table_container;
                const start = hasContainer ? pos : pos + 1;
                const end = start + (hasContainer ? containingNode.nodeSize : table.node.nodeSize);
                tr.deleteRange(start, end);
                setTextSelection(start, 1)(tr);
              }
            });
          },
        },
      ];
    },
  };
};

export default extension;
