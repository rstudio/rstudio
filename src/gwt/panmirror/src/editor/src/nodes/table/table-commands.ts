/*
 * table-commands.ts
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
import { EditorState, Transaction } from 'prosemirror-state';
import { findParentNodeOfType, setTextSelection, findChildrenByType } from 'prosemirror-utils';
import { isInTable, Rect, TableMap, selectionCell, CellSelection, toggleHeader } from 'prosemirror-tables';

import { InsertTableFn } from '../../api/ui';
import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { EditorView } from 'prosemirror-view';
import { canInsertNode } from '../../api/node';

export function insertTable(onInsertTable: InsertTableFn) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
    const schema = state.schema;

    // can we insert?
    if (!canInsertNode(state, schema.nodes.table_container)) {
      return false;
    }

    // is the selection inside a table caption? if it is then we can't insert
    // as it will "split" the table_container in such a way that an invalid
    // table will be created
    if (findParentNodeOfType(schema.nodes.table_caption)(state.selection)) {
      return false;
    }

    async function asyncInsertTable() {
      if (dispatch) {
        const result = await onInsertTable();
        if (result) {
          // create cells
          const rows: ProsemirrorNode[] = [];
          for (let r = 0; r < result.rows; r++) {
            const cells: ProsemirrorNode[] = [];
            const cellType = r === 0 && result.header ? schema.nodes.table_header : schema.nodes.table_cell;
            for (let c = 0; c < result.cols; c++) {
              cells.push(cellType.createAndFill({}, schema.nodes.paragraph.create()!)!);
            }
            rows.push(schema.nodes.table_row.createAndFill({}, cells)!);
          }

          // create table
          const table = schema.nodes.table.createAndFill({}, rows)!;
          const tableCaption = schema.nodes.table_caption.createAndFill(
            { inactive: !result.caption },
            result.caption ? schema.text(result.caption) : undefined,
          )!;
          const tableContainer = schema.nodes.table_container.createAndFill({}, [table, tableCaption])!;

          // insert
          const tr = state.tr;
          tr.replaceSelectionWith(tableContainer);

          // select first cell
          const selectionPos = tr.mapping.map(state.selection.from, -1);
          setTextSelection(selectionPos)(tr).scrollIntoView();

          // dispatch & focus
          dispatch(tr);
        }
        if (view) {
          view.focus();
        }
      }
    }
    asyncInsertTable();

    return true;
  };
}

export function deleteTable() {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {
    const schema = state.schema;
    const container = findParentNodeOfType(schema.nodes.table_container)(state.selection);
    if (container) {
      if (dispatch) {
        const tr = state.tr;
        tr.delete(container.pos, container.pos + container.node.nodeSize);
        dispatch(tr.scrollIntoView());
      }
      return true;
    }
    return false;
  };
}

export function deleteTableCaption() {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {
    // must be a selection within an empty table caption
    const schema = state.schema;
    const { $head } = state.selection;
    if ($head.parent.type !== schema.nodes.table_caption || $head.parent.childCount !== 0) {
      return false;
    }

    if (dispatch) {
      // set the caption to inactive
      const tr = state.tr;
      const caption = $head.parent;
      tr.setNodeMarkup($head.pos - 1, schema.nodes.table_caption, {
        ...caption.attrs,
        inactive: true,
      });
      setTextSelection($head.pos - 1, -1)(tr);
      dispatch(tr);
    }

    return true;
  };
}

export class TableToggleHeaderCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.TableToggleHeader, [], toggleHeader('row'));
  }

  public isActive(state: EditorState): boolean {
    if (!isInTable(state)) {
      return false;
    }
    const { table } = selectedRect(state);
    const firstCell = table.firstChild!.firstChild!;
    return firstCell.type === state.schema.nodes.table_header;
  }
}

export class TableToggleCaptionCommand extends ProsemirrorCommand {
  constructor() {
    super(
      EditorCommandId.TableToggleCaption,
      [],
      (state: EditorState, dispatch?: (tr: Transaction<any>, view?: EditorView) => void) => {
        if (!isInTable(state)) {
          return false;
        }
        const caption = this.tableCaptionNode(state);
        if (!caption) {
          return false;
        }

        if (dispatch) {
          const focus = caption.node.attrs.inactive;
          const tr = state.tr;
          tr.setNodeMarkup(caption.pos + 1, state.schema.nodes.table_caption, {
            ...caption.node.attrs,
            inactive: !caption.node.attrs.inactive,
          });
          if (focus) {
            setTextSelection(caption.pos + 1)(tr).scrollIntoView();
          }
          dispatch(tr);
        }

        return true;
      },
    );
  }

  public isActive(state: EditorState): boolean {
    if (!isInTable(state)) {
      return false;
    }

    const caption = this.tableCaptionNode(state);
    if (!caption) {
      return false;
    }

    return !caption.node.attrs.inactive;
  }

  private tableCaptionNode(state: EditorState) {
    const container = findParentNodeOfType(state.schema.nodes.table_container)(state.selection);
    if (container) {
      const caption = findChildrenByType(container.node, state.schema.nodes.table_caption);
      return {
        node: caption[0].node,
        pos: container.pos + caption[0].pos,
      };
    } else {
      return undefined;
    }
  }
}

export enum CssAlignment {
  Left = 'left',
  Right = 'right',
  Center = 'center',
}

export class TableColumnAlignmentCommand extends ProsemirrorCommand {
  private readonly align: CssAlignment | null;

  constructor(id: EditorCommandId, align: CssAlignment | null) {
    super(id, [], (state: EditorState, dispatch?: (tr: Transaction<any>) => void) => {
      if (!isInTable(state)) {
        return false;
      }

      if (dispatch) {
        const { table, tableStart, left } = selectedRect(state);
        const tr = state.tr;
        table.forEach((rowNode, rowOffset) => {
          rowNode.forEach((cellNode, cellOffset, i) => {
            if (i === left) {
              const cellPos = tableStart + 1 + rowOffset + cellOffset;
              tr.setNodeMarkup(cellPos, cellNode.type, {
                ...cellNode.attrs,
                align,
              });
            }
          });
        });
        dispatch(tr);
      }

      return true;
    });
    this.align = align;
  }

  public isActive(state: EditorState): boolean {
    if (!isInTable(state)) {
      return false;
    }
    const { table, top, left } = selectedRect(state);
    const cell = table.child(top).child(left);
    return cell.attrs.align === this.align;
  }
}

// from: https://github.com/ProseMirror/prosemirror-tables/blob/master/src/commands.js

interface SelectedRect extends Rect {
  tableStart: number;
  map: TableMap;
  table: ProsemirrorNode;
}

function selectedRect(state: EditorState): SelectedRect {
  const sel = state.selection;
  const $pos = selectionCell(state)!;
  const table = $pos.node(-1);
  const tableStart = $pos.start(-1);
  const map = TableMap.get(table);
  let rect: Rect;
  if (sel instanceof CellSelection) {
    rect = map.rectBetween(sel.$anchorCell.pos - tableStart, sel.$headCell.pos - tableStart);
  } else {
    rect = map.findCell($pos.pos - tableStart);
  }
  return {
    ...rect,
    tableStart,
    map,
    table,
  };
}
