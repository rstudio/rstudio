
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

import { EditorState } from "prosemirror-state";
import { findParentNodeOfType } from "prosemirror-utils";

export interface EditorRmdChunk {
  lang: string;
  meta: string;
  code: string;
}

export type ExecuteRmdChunkFn = (chunk: EditorRmdChunk) => void;

export function activeRmdChunk(state: EditorState) : EditorRmdChunk | null {
  if (state.schema.nodes.rmd_chunk) {
    const rmdNode = findParentNodeOfType(state.schema.nodes.rmd_chunk)(state.selection);
    if (rmdNode) {
      return rmdChunk(rmdNode.node.textContent);
    } 
  } 
  return null;
}

export function rmdChunk(code: string) : EditorRmdChunk | null {
  const lines = code.split('\n');
  if (lines.length > 0) {
    const meta = lines[0].replace(/^.*?\{([^}]*)\}.*?$/, '$1');
    const matchLang = meta.match(/\w+/);
    const lang = matchLang ? matchLang[0] : '';
    return {
      lang,
      meta,
      code: lines.slice(1).join('\n')
    };
  } else {
    return null;
  }
}