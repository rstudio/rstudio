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

const TABLE_CAPTION = 0;
const TABLE_COL_ALIGNMENTS = 1;
const TABLE_COL_WIDTHS = 2;
const TABLE_HEADER = 3;
const TABLE_ROWS = 4;

export function readPandocTable(schema: Schema) {
  return (writer: ProsemirrorWriter, tok: PandocToken) => {
    // get alignments and columns widths
    const alignments = columnCssAlignments(tok);
    const colpercents = tok.c[TABLE_COL_WIDTHS] as number[];

    // helper function to parse a table row
    const parseRow = (row: PandocToken[][], cellType: NodeType) => {
      if (row.length) {
        writer.openNode(schema.nodes.table_row, {});
        row.forEach((cell, i) => {
          writer.openNode(cellType, { align: alignments[i] });
          writer.writeTokens(cell);
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
    const headers = tok.c[TABLE_HEADER] as PandocToken[][];
    if (headers.some(header => header.length > 0)) {
      parseRow(headers, schema.nodes.table_header);
    }

    // parse table rows
    const rows = tok.c[TABLE_ROWS] as PandocToken[][][];
    rows.forEach(row => {
      parseRow(row, schema.nodes.table_cell);
    });

    // close table node
    writer.closeNode();

    // read caption
    const caption = tok.c[TABLE_CAPTION];
    writer.openNode(schema.nodes.table_caption, { inactive: caption.length === 0 });
    writer.writeTokens(tok.c[TABLE_CAPTION]);
    writer.closeNode();

    // close table container node
    writer.closeNode();
  };
}

export function writePandocTableContainer(output: PandocOutput, node: ProsemirrorNode) {
  const caption = node.lastChild!;
  const table = node.firstChild!;

  output.writeToken(PandocTokenType.Table, () => {
    // write caption
    output.writeNode(caption);

    // write table
    output.writeNode(table);
  });
}

export function writePandocTable(output: PandocOutput, node: ProsemirrorNode) {
  const firstRow = node.firstChild!;
  const cols = firstRow.childCount;

  // write alignments
  const alignments = tableColumnAlignments(node);
  output.writeArray(() => {
    for (let i = 0; i < cols; i++) {
      output.writeToken(alignments[i]);
    }
  });

  // write columns widths
  const widths = tableColumnWidths(node);
  output.writeArray(() => {
    for (let i = 0; i < cols; i++) {
      output.write(widths[i]);
    }
  });

  // write header row if necessary
  let headerCut = 0;
  if (firstRow.firstChild!.type === node.type.schema.nodes.table_header) {
    output.writeNode(firstRow);
    headerCut = 1;
  } else {
    output.writeArray(() => {
      output.writeInlines(Fragment.empty);
    });
  }

  // write rows
  output.writeArray(() => {
    for (let i = headerCut; i < node.childCount; i++) {
      output.writeNode(node.content.child(i));
    }
  });
}

export function writePandocTableCaption(output: PandocOutput, node: ProsemirrorNode) {
  output.writeArray(() => {
    if (!node.attrs.inactive) {
      output.writeInlines(node.content);
    }
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

function columnCssAlignments(tableToken: PandocToken) {
  return tableToken.c[TABLE_COL_ALIGNMENTS].map((alignment: any) => {
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
