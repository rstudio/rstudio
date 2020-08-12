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

export const kCharClassWord = 0;
export const kCharClassBoundary = 1;
export const kCharClassNonWord = 2;

export interface EditorUISpelling {
  // realtime interface
  realtimeEnabled: () => boolean;
  checkWord: (word: string) => boolean;
  suggestionList: (word: string) => string[];

  // dictionary
  isWordIgnored: (word: string) => boolean;
  ignoreWord: (word: string) => void;
  unignoreWord: (word: string) => void;
  addToDictionary: (word: string) => void;

  // word breaking
  breakWords: (text: string) => EditorWordRange[];
  classifyCharacter: (ch: number) => number;
}

export interface EditorAnchor {
  getPosition: () => number;
}

export interface EditorRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface EditorWordRange {
  start: number;
  end: number;
}

export interface EditorWordSource {
  hasNext: () => boolean;
  next: () => EditorWordRange | null;
}

export interface EditorSpellingDoc {

  getWords: (start: number, end: number) => EditorWordSource;

  createAnchor: (pos: number) => EditorAnchor;

  shouldCheck: (wordRange: EditorWordRange) => boolean;
  setSelection: (wordRange: EditorWordRange) => void;
  getText: (wordRange: EditorWordRange) => string;

  getCursorPosition: () => number;
  replaceSelection: (text: string) => void;
  getSelectionStart: () => number;
  getSelectionEnd: () => number;

  getCursorBounds: () => EditorRect;
  moveCursorNearTop: () => void;

  dispose: () => void;

}

