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

import { MarkType, Schema, Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorState } from 'prosemirror-state';

import { EditorWordSource, EditorWordRange, EditorUISpelling, kCharClassNonWord } from "../../api/spelling";
import { PandocMark } from '../../api/mark';

export const beginDocPos = () => 1;
export const endDocPos = (doc: ProsemirrorNode) => doc.nodeSize - 2;

export function getWords(
  state: EditorState,
  start: number,
  end: number | null,
  spelling: EditorUISpelling,
  excluded: MarkType[],
): EditorWordSource {

  // provide default for end
  if (end === null) {
    end = endDocPos(state.doc);
  }

  // enlarge range to begin/end 
  const beginPos = findBeginWord(state, start, spelling.classifyCharacter);
  const endPos = findEndWord(state, end, spelling.classifyCharacter);

  const words: EditorWordRange[] = [];
  let currentPos = beginPos;
  while (currentPos <= endPos) {

    // advance until we find a word
    currentPos = advanceToWord(state, currentPos, spelling.classifyCharacter);
    if (currentPos >= endPos) {
      break;
    }

    // find end of word
    const endWordPos = findEndWord(state, currentPos, spelling.classifyCharacter);

    // add word if it doesn't have an excluded type
    if (!excluded.some(markType => state.doc.rangeHasMark(currentPos, endWordPos, markType))) {
      const wordsText = state.doc.textBetween(currentPos, endWordPos);
      words.push(...spelling.breakWords(wordsText).map(wordRange => {
        return {
          start: currentPos + wordRange.start,
          end: currentPos + wordRange.end
        };
      }));
    }

    // next word
    currentPos = endWordPos;
  }

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

export function advanceToWord(state: EditorState, pos: number, classifier: (ch: number) => number) {
  while (pos < endDocPos(state.doc)) {
    const nextChar = charAt(state.doc, pos);
    if (classifier(nextChar) !== kCharClassNonWord) {
      break;
    } else {
      pos++;
    }
  }
  return pos;
}

export function findBeginWord(state: EditorState, pos: number, classifier: (ch: number) => number) {
  // scan backwards until a non-word character is encountered
  while (true) {
    const prevChar = charAt(state.doc, pos - 1);
    if (classifier(prevChar) === kCharClassNonWord) {
      break;
    } else {
      pos--;
    }
  }
  // return the position
  return pos;
}

export function findEndWord(state: EditorState, pos: number, classifier: (ch: number) => number) {
  // scan forwards until a non-word character is encountered
  while (pos < endDocPos(state.doc)) {
    const nextChar = charAt(state.doc, pos);
    if (classifier(nextChar) === kCharClassNonWord) {
      break;
    } else {
      pos++;
    }
  }

  // return the position
  return pos;
}

// get the chracter code at the specified position, returning character code 32 (a space)
// for begin/end of document, block boundaries, and non-text leaf nodes
function charAt(doc: ProsemirrorNode, pos: number) {
  if (pos < beginDocPos() || pos >= (endDocPos(doc))) {
    return 32; // space for doc boundary
  } else {
    return (doc.textBetween(pos, pos + 1, ' ', ' ') || ' ').charCodeAt(0);
  }
}

export function excludedMarks(schema: Schema, marks: readonly PandocMark[]): MarkType[] {
  return marks
    .filter(mark => mark.noSpelling)
    .map(mark => schema.marks[mark.name]);
}

export function spellcheckerWord(word: string) {
  return word.replace(/’/g, '\'');
}

export function editorWord(word: string) {
  return word.replace(/'/g, '’');
}

