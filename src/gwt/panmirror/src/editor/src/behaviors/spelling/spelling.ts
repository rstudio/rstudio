/*
 * spelling.ts
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

import { MarkType, Schema } from 'prosemirror-model';
import { EditorState } from 'prosemirror-state';

import { EditorWordSource, EditorWordRange } from "../../api/spelling";
import { TextWithPos } from "../../api/text";
import { PandocMark } from '../../api/mark';

// TODO: more efficient / incremntal chekcing
// TODO: node that selection changed can invalidatee the suppresed decoration at the cursor 
// TODO: deal with checking across mark boundaries

// TODO: themed underline color

export function getWords(
  state: EditorState,
  start: number,
  end: number | null,
  wordBreaker: (text: string) => EditorWordRange[],
  excluded: MarkType[]
): EditorWordSource {

  // provide default for end
  if (end === null) {
    end = state.doc.nodeSize - 2;
  }

  // examine every text node
  const textNodes: TextWithPos[] = [];
  state.doc.nodesBetween(start, end, (node, pos, parent) => {
    if (node.isText && !parent.type.spec.code) {
      // filter on marks where we shouldn't check spelling (e.g. url, code)
      if (!excluded.some((markType: MarkType) => markType.isInSet(node.marks))) {
        textNodes.push({ text: node.textContent, pos });
      }
    }
  });

  // create word ranges
  const words: EditorWordRange[] = [];
  textNodes.forEach(text => {
    if (text.pos >= start && text.pos < end!) {
      words.push(...wordBreaker(text.text).map(wordRange => {
        return {
          start: text.pos + wordRange.start,
          end: text.pos + wordRange.end
        };
      }));
    }
  });

  // return iterator over word range
  return {
    hasNext: () => {
      return words.length > 0;
    },
    next: () => {
      if (words.length > 0) {
        return words.shift()!;
      } else {
        return null;
      }
    }
  };
}

export function spellcheckerWord(word: string) {
  return word.replace(/’/g, '\'');
}

export function editorWord(word: string) {
  return word.replace(/'/g, '’');
}

export function excludedMarks(schema: Schema, marks: readonly PandocMark[]): MarkType[] {
  return marks
    .filter(mark => mark.noSpelling)
    .map(mark => schema.marks[mark.name]);
}
