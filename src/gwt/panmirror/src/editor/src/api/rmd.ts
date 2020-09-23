/*
 * rmd.ts
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

import { Node as ProsemirrorNode, NodeType } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import {
  findParentNodeOfType,
  findChildrenByType,
  findChildren,
  findChildrenByMark,
  setTextSelection,
} from 'prosemirror-utils';

import { getMarkRange } from './mark';
import { precedingListItemInsertPos, precedingListItemInsert } from './list';
import { toggleBlockType } from './command';
import { selectionIsBodyTopLevel } from './selection';
import { uuidv4 } from './util';

export interface EditorRmdChunk {
  lang: string;
  meta: string;
  code: string;
}

export type ExecuteRmdChunkFn = (chunk: EditorRmdChunk) => void;

export function canInsertRmdChunk(state: EditorState) {

  // must either be at the body top level, within a list item, within a div, or within a
  // blockquote (and never within a table)
  const schema = state.schema;
  const within = (nodeType: NodeType) => !!findParentNodeOfType(nodeType)(state.selection);
  if (within(schema.nodes.table)) {
    return false;
  }
  if (
    !selectionIsBodyTopLevel(state.selection) &&
    !within(schema.nodes.list_item) &&
    !within(schema.nodes.blockquote) &&
    (schema.nodes.div && !within(schema.nodes.div))

  ) {
    return false;
  }

  return true;
}

export function insertRmdChunk(chunkPlaceholder: string, rowOffset = 0, colOffset = 0) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
    const schema = state.schema;

    if (
      !toggleBlockType(schema.nodes.rmd_chunk, schema.nodes.paragraph)(state) &&
      !precedingListItemInsertPos(state.doc, state.selection)
    ) {
      return false;
    }

    // must either be at the body top level, within a list item, within a div, or within a
    // blockquote (and never within a table)
    if (!canInsertRmdChunk(state)) {
      return false;
    }

    if (dispatch) {
      // compute offset
      const lines = chunkPlaceholder.split(/\r?\n/);
      const lineChars = lines.slice(0, rowOffset).reduce((count, line) => count + line.length + 1, 1);
      const offsetChars = lineChars + colOffset;

      // perform insert
      const tr = state.tr;
      const rmdText = schema.text(chunkPlaceholder);
      const rmdNode = schema.nodes.rmd_chunk.create({ navigation_id: uuidv4() }, rmdText);
      const prevListItemPos = precedingListItemInsertPos(tr.doc, tr.selection);
      if (prevListItemPos) {
        precedingListItemInsert(tr, prevListItemPos, rmdNode);
      } else {
        tr.replaceSelectionWith(rmdNode);
        const selPos = tr.selection.from - rmdNode.nodeSize - 1 + offsetChars - 1;
        setTextSelection(selPos)(tr);
      }

      dispatch(tr);
    }

    return true;
  };
}

export function activeRmdChunk(state: EditorState): EditorRmdChunk | null {
  if (state.schema.nodes.rmd_chunk) {
    const rmdNode = findParentNodeOfType(state.schema.nodes.rmd_chunk)(state.selection);
    if (rmdNode) {
      return rmdChunk(rmdNode.node.textContent);
    }
  }
  return null;
}

export function previousExecutableRmdChunks(state: EditorState, pos = state.selection.from): EditorRmdChunk[] {
  const activeChunk = activeRmdChunk(state);
  const lang = activeChunk ? activeChunk.lang : 'r';
  const kEvalFalseRegEx = /eval\s*=\s*F(?:ALSE)?/;
  return previousRmdChunks(state, pos, chunk => {
    return (
      chunk.lang.localeCompare(lang, undefined, { sensitivity: 'accent' }) === 0 && !kEvalFalseRegEx.test(chunk.meta)
    );
  });
}

export function previousRmdChunks(state: EditorState, pos: number, filter?: (chunk: EditorRmdChunk) => boolean) {
  // chunks to return
  const chunks: EditorRmdChunk[] = [];

  // find all chunks in the document and return ones before the position that pass the specified filter
  const schema = state.schema;
  const rmdChunkNodes = findChildrenByType(state.doc, schema.nodes.rmd_chunk);
  for (const rmdChunkNode of rmdChunkNodes) {
    if (rmdChunkNode.pos + rmdChunkNode.node.nodeSize > pos) {
      break;
    }
    const chunk = rmdChunk(rmdChunkNode.node.textContent);
    if (chunk && (!filter || filter(chunk))) {
      chunks.push(chunk);
    }
  }

  // return chunks
  return chunks;
}

export function rmdChunk(code: string): EditorRmdChunk | null {
  const lines = code.trimLeft().split('\n');
  if (lines.length > 0) {
    const meta = lines[0].replace(/^[\s`\{]*(.*?)\}?\s*$/, '$1');
    const matchLang = meta.match(/\w+/);
    const lang = matchLang ? matchLang[0] : '';

    // a completely empty chunk (no second line) should be returned
    // as such. if it's not completely empty then append a newline
    // to the result of split (so that the chunk ends w/ a newline)
    const chunkCode = lines.length > 1
      ? (lines.slice(1).join('\n') + '\n')
      : '';

    return {
      lang,
      meta,
      code: chunkCode,
    };
  } else {
    return null;
  }
}

export function mergeRmdChunks(chunks: EditorRmdChunk[]) {
  if (chunks.length) {
    const merged = {
      lang: chunks[0].lang,
      meta: '',
      code: '',
    };
    chunks.forEach(chunk => (merged.code += chunk.code + '\n'));
    return merged;
  } else {
    return null;
  }
}

export function rmdChunkEngineAndLabel(text: string) {
  const match = text.match(/^\{([a-zA-Z0-9_]+)[\s,]+([a-zA-Z0-9/-]+)/);
  if (match) {
    return {
      engine: match[1],
      label: match[2]
    };
  } else {
    return null;
  }
}

export function haveTableCellsWithInlineRcode(doc: ProsemirrorNode) {
  const schema = doc.type.schema;
  const haveRCode = !!doc.type.schema.nodes.rmd_chunk;
  if (haveRCode) {
    const isTableCell = (node: ProsemirrorNode) =>
      node.type === schema.nodes.table_cell || node.type === schema.nodes.table_header;
    return findChildren(doc, isTableCell).some(cell => {
      if (doc.rangeHasMark(cell.pos, cell.pos + cell.node.nodeSize, schema.marks.code)) {
        const markedNodes = findChildrenByMark(cell.node, schema.marks.code, true);
        return markedNodes.some(markedNode => {
          const from = cell.pos + 1 + markedNode.pos;
          const markedRange = getMarkRange(doc.resolve(from), schema.marks.code);
          if (markedRange) {
            const text = doc.textBetween(markedRange.from, markedRange.to);
            return /^r[ #].+$/.test(text);
          } else {
            return false;
          }
        });
      } else {
        return false;
      }
    });
  } else {
    return false;
  }
}
