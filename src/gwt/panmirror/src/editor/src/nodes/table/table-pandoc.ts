/*
 * table-pandoc.ts
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

import { Schema, NodeType, Node as ProsemirrorNode, Fragment } from 'prosemirror-model';

import { ProsemirrorWriter, PandocToken, PandocTokenType, PandocOutput } from '../../api/pandoc';

import { CssAlignment } from './table-commands';
import { tableColumnAlignments, tableColumnWidths } from './table-columns';

// attributes
const kTableAttr = 0;

// caption
const kTableCaption = 1;
const kTableCaptionShort = 0; // [Inline]
const kTableCaptionFull = 1;  // [Block]

// columdefs
const kTableColSpec = 2;
const kTableColSpecAlign = 0;
const kTableColSpecWidth = 1;

// table head
const kTableHead = 3;
const kTableHeadAttr = 0;
const kTableHeadRows = 1; // [Row]

// table body
const kTableBody = 4;
const kTableBodyAttr = 0;
const kTableBodyRowHeadNumColumns = 1;
const kTableBodyRowHead = 2;
const kTableBodyRows = 3; // [Row]

// table foot
const kTableFoot = 5;
const kTableFootAttr = 0;
const kTableFootRows = 1; // [Row]

// table row
const kTableRowAttr = 0;
const kTableRowCells = 1; // [Cell]

// table cell
const kTableCellAttr = 0;
const kTableCellAlignments = 1;
const kTableCellRowSpan = 2;
const kTableCellColSpan = 3;
const KTableCellContents = 4; // [Block]


export function readPandocTable(schema: Schema) {
  return (writer: ProsemirrorWriter, tok: PandocToken) => {
    // get alignments and columns widths
    const alignments = columnCssAlignments(tok);
    const colpercents = columnPercents(tok);

    // helper function to parse a table row
    const parseRow = (row: PandocToken, cellType: NodeType) => {
      const cells: PandocToken[] = row.c[kTableRowCells];
      if (cells.length) {
        writer.openNode(schema.nodes.table_row, {});
        cells.forEach((cell: PandocToken, i) => {
          writer.openNode(cellType, { align: alignments[i] });
          writer.writeTokens(cell.c[KTableCellContents]);
          writer.closeNode();
        });
        writer.closeNode();
      }
    };

    // open table container node
    writer.openNode(schema.nodes.table_container, {});

    // open table node
    writer.openNode(schema.nodes.table, { colpercents });

    // parse column headers
    const head = tok.c[kTableHead] as PandocToken;
    const firstRow = head.c[kTableHeadRows][0];
    if (firstRow && firstRow.c[kTableRowCells].some((cell: PandocToken) => cell.c[KTableCellContents].length > 0)) {
      parseRow(firstRow, schema.nodes.table_header);
    }

    // parse table rows
    const body = tok.c[kTableBody][0] as PandocToken;
    body.c[kTableBodyRows].forEach((row: PandocToken) => {
      parseRow(row, schema.nodes.table_cell);
    });

    // close table node
    writer.closeNode();

    // read caption
    const caption = tok.c[kTableCaption].c[kTableCaptionFull];
    const captionBlock: PandocToken[] = caption.length ? caption[0].c : [];
    writer.openNode(schema.nodes.table_caption, { inactive: captionBlock.length === 0 });
    writer.writeTokens(captionBlock);
    writer.closeNode();

    // close table container node
    writer.closeNode();
  };
}

export function writePandocTableContainer(output: PandocOutput, node: ProsemirrorNode) {
  const caption = node.lastChild!;
  const table = node.firstChild!;

  output.writeToken(PandocTokenType.Table, () => {

    // write empty attributes
    output.writeAttr();

    // write caption
    output.writeNode(caption);

    // write table
    output.writeNode(table);
  });
}

export function writePandocTable(output: PandocOutput, node: ProsemirrorNode) {
  const firstRow = node.firstChild!;

  // get alignments and column widths
  const alignments = tableColumnAlignments(node);
  const widths = tableColumnWidths(node);

  // write colspcs
  // TODO: Columns are coming out ColWidthDefault
  output.writeArray(() => {
    alignments.forEach((align, i) => {
      output.writeArray(() => {
        output.writeToken(align);
        if (widths[i] === 0) {
          output.writeToken(PandocTokenType.ColWidthDefault);
        } else {
          output.writeToken(PandocTokenType.ColWidth, widths[i]);
        }
      });
    });
  });


  // write header row if necessary
  const headerCut = firstRow.firstChild!.type === node.type.schema.nodes.table_header ? 1 : 0;
  output.writeToken(PandocTokenType.TableHead, () => {
    output.writeAttr();
    output.writeArray(() => {
      writePandocTableRow(output, firstRow, headerCut === 0);
    });
  });

  // write table body
  output.writeArray(() => {
    output.writeToken(PandocTokenType.TableBody, () => {
      output.writeAttr();
      output.writeToken(PandocTokenType.RowHeadColumns, 0);
      output.writeArray(() => { /* */ });
      // write rows
      output.writeArray(() => {
        for (let i = headerCut; i < node.childCount; i++) {
          writePandocTableRow(output, node.content.child(i));
        }
      });
    });
  });

  // write table footer
  output.writeToken(PandocTokenType.TableFoot, () => {
    output.writeAttr();
    output.writeArray(() => { /* */ });
  });


}

export function writePandocTableCaption(output: PandocOutput, node: ProsemirrorNode) {
  output.writeToken(PandocTokenType.Caption, () => {
    output.write(null);
    output.writeArray(() => {
      if (!node.attrs.inactive && node.childCount > 0) {
        output.writeToken(PandocTokenType.Plain, () => {
          output.writeInlines(node.content);
        });
      }
    });
  });
}

export function writePandocTableNodes(output: PandocOutput, node: ProsemirrorNode) {
  output.writeArray(() => {
    output.writeNodes(node);
  });
}

export function writePandocTableHeaderNodes(output: PandocOutput, node: ProsemirrorNode) {
  output.writeArray(() => {
    if (node.textContent.length > 0) {
      output.writeNodes(node);
    } else {
      // write a paragraph containing a space (this is an attempt to fix an issue where
      // empty headers don't get correct round-tripping)
      output.writeToken(PandocTokenType.Para, () => {
        output.writeRawMarkdown(' ');
      });
    }
  });
}

function writePandocTableRow(output: PandocOutput, node: ProsemirrorNode, empty = false) {
  output.writeToken(PandocTokenType.Row, () => {
    output.writeAttr();
    output.writeArray(() => {
      node.forEach((cellNode) => {
        output.writeToken(PandocTokenType.Cell, () => {
          output.writeAttr();
          output.writeToken(PandocTokenType.AlignDefault);
          output.writeToken(PandocTokenType.RowSpan, 1);
          output.writeToken(PandocTokenType.ColSpan, 1);
          if (!empty) {
            output.writeNode(cellNode);
          } else {
            output.writeArray(() => {
              output.writeInlines(Fragment.empty);
            });
          }
        });
      });
    });
  });
}


function columnCssAlignments(tableToken: PandocToken) {
  return tableToken.c[kTableColSpec].map((spec: any) => {
    const alignment = spec[kTableColSpecAlign];
    switch (alignment.t) {
      case PandocTokenType.AlignLeft:
        return CssAlignment.Left;
      case PandocTokenType.AlignRight:
        return CssAlignment.Right;
      case PandocTokenType.AlignCenter:
        return CssAlignment.Center;
      case PandocTokenType.AlignDefault:
      default:
        return null;
    }
  });
}

function columnPercents(tableToken: PandocToken): number[] {
  return tableToken.c[kTableColSpec].map((spec: any) => {
    const width = spec[kTableColSpecWidth];
    return width.t === PandocTokenType.ColWidth ? width.c : 0;
  });
}
