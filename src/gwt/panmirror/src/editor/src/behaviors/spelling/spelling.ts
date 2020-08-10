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

import uniqby from 'lodash.uniqby';

import { EditorWordSource, EditorWordRange, EditorUISpelling, kCharClassNonWord, kCharClassBoundary, kCharClassWord } from "../../api/spelling";
import { TextWithPos } from "../../api/text";
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
  // examine every text node in the range
  const textNodes: TextWithPos[] = [];
  state.doc.nodesBetween(start, end, (node, pos, parent) => {
    if (node.isText && !parent.type.spec.code) {
      textNodes.push({ text: node.textContent, pos });
    }
  });

  // create word ranges
  let words: EditorWordRange[] = [];
  textNodes.forEach(text => {

    // we need to extend the range forward and back to capture all word characters (otherwise we 
    // could end up checking 'half words' if the word included a mark or the cursor started out
    // in the middle of a word)
    const beginPos = findBeginWord(state, text.pos, spelling.classifyCharacter);
    const endPos = findEndWord(state, text.pos, spelling.classifyCharacter);

    // verify it overlaps w/ the original range
    if (beginPos >= start && beginPos < end! || endPos >= start && endPos < end!) {
      // add the word so long as it's range doesn't overlap w/ an excluded mark
      if (!excluded.some(markType => state.doc.rangeHasMark(beginPos, endPos, markType))) {
        const wordsText = state.doc.textBetween(beginPos, endPos);
        words.push(...spelling.breakWords(wordsText).map(wordRange => {
          return {
            start: beginPos + wordRange.start,
            end: beginPos + wordRange.end
          };
        }));
      }
    }



  });

  // remove duplicate words (could occur due to a word being split into 2 text
  // nodes b/c of a mark right in the middle of the word)
  words = uniqby(words, word => word.start);

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

export function hasExcludedMark(state: EditorState, from: number, to: number, excluded: MarkType[]) {
  const $from = state.doc.resolve(from);
  const $to = state.doc.resolve(to);
  const rangeMarks = $from.marksAcross($to) || [];
  return rangeMarks.some(mark => excluded.includes(mark.type));
}


function findBeginWord(state: EditorState, pos: number, classifier: (ch: number) => number) {

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

function findEndWord(state: EditorState, pos: number, classifier: (ch: number) => number) {

  // scan forwards until a non-word character is encountered
  while (true) {
    const nextChar = charAt(state.doc, pos);
    if (classifier(nextChar) === kCharClassNonWord) {
      break;
    } else {
      pos++;
    }
  }

  // back over boundary characters
  while (charAt(state.doc, pos - 1) === kCharClassBoundary) {
    pos--;
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
