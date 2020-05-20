/*
 * rmd.ts
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
import { EditorState } from 'prosemirror-state';

import { findParentNodeOfType, findChildrenByType, findChildren, findChildrenByMark } from 'prosemirror-utils';

import { getMarkRange } from './mark';

export interface EditorRmdChunk {
  lang: string;
  meta: string;
  code: string;
}

export type ExecuteRmdChunkFn = (chunk: EditorRmdChunk) => void;

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
  const lines = code.split('\n');
  if (lines.length > 0) {
    const meta = lines[0].replace(/^.*?\{([^}]*)\}.*?$/, '$1');
    const matchLang = meta.match(/\w+/);
    const lang = matchLang ? matchLang[0] : '';
    return {
      lang,
      meta,
      code: lines.slice(1).join('\n'),
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
          }
        });
      }
    });
  } else {
    return false;
  }
}
