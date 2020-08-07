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

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorView } from "prosemirror-view";
import { TextSelection } from 'prosemirror-state';

import { EditorWordBreaker, EditorWordSource, EditorWordRange, EditorAnchor, EditorRect, EditorSpellingDoc } from "../api/spelling";
import { mergedTextNodes } from "../api/text";

export function getSpellingDoc(view: EditorView, wordBreaker: EditorWordBreaker): EditorSpellingDoc {

  return {

    getWords: (start: number, end: number | null): EditorWordSource => {

      // provide default for end
      if (end === null) {
        end = view.state.doc.nodeSize;
      }

      // examine text nodes in range
      const textNodes = mergedTextNodes(view.state.doc, (node: ProsemirrorNode, pos: number) => {

        // filter on code nodes
        if (node.type.spec.code) {
          return false;
        }

        // filter on nodes overlapping w/ requested range
        const startPos = pos;
        const endPos = pos + node.nodeSize;
        return !((startPos < start && endPos < start) || startPos > end!);
      });

      // create word ranges
      const words: EditorWordRange[] = [];
      textNodes.forEach(text => {
        words.push(...wordBreaker(text.text).map(wordRange => {
          return {
            start: text.pos + wordRange.start,
            end: text.pos + wordRange.end
          };
        }));
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
    },

    createAnchor: (pos: number): EditorAnchor => {
      // TODO: use plugin to map pos across transactions
      return {
        getPosition: () => pos
      };
    },

    shouldCheck: (wordRange: EditorWordRange): boolean => {
      return true;
    },

    setSelection: (wordRange: EditorWordRange) => {
      const tr = view.state.tr;
      tr.setSelection(TextSelection.create(tr.doc, wordRange.start, wordRange.end)).scrollIntoView();
      view.dispatch(tr);
    },

    getText: (wordRange: EditorWordRange): string => {
      return view.state.doc.textBetween(wordRange.start, wordRange.end);
    },

    getCursorPosition: (): number => {
      return view.state.selection.head;
    },

    replaceSelection: (text: string) => {
      const tr = view.state.tr;
      tr.replaceSelectionWith(view.state.schema.text(text), true);
      view.dispatch(tr);
    },

    getSelectionStart: (): number => {
      return view.state.selection.from;
    },

    getSelectionEnd: (): number => {
      return view.state.selection.to;
    },

    getCursorBounds: (): EditorRect => {

      const fromCoords = view.coordsAtPos(view.state.selection.from);
      const toCoords = view.coordsAtPos(view.state.selection.to);

      return {
        x: Math.min(fromCoords.left, toCoords.left),
        y: fromCoords.top,
        width: Math.abs(fromCoords.left - toCoords.left),
        height: toCoords.bottom - fromCoords.top
      };
    },

    moveCursorNearTop: () => {
      const tr = view.state.tr;
      tr.scrollIntoView();
      view.dispatch(tr);
    }

  };
}

