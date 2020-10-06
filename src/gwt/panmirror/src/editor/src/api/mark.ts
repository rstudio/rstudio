/*
 * mark.ts
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

import { Mark, MarkSpec, MarkType, ResolvedPos, Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorState, Selection, Transaction } from 'prosemirror-state';

import { PandocTokenReader, PandocMarkWriterFn, PandocInlineHTMLReaderFn } from './pandoc';
import { mergedTextNodes } from './text';
import { findChildrenByMark } from 'prosemirror-utils';
import { MarkTransaction } from './transaction';

export interface PandocMark {
  readonly name: string;
  readonly spec: MarkSpec;
  readonly noInputRules?: boolean;
  readonly noSpelling?: boolean;
  readonly pandoc: {
    readonly readers: readonly PandocTokenReader[];
    readonly inlineHTMLReader?: PandocInlineHTMLReaderFn;
    readonly writer: {
      priority: number;
      write: PandocMarkWriterFn;
    };
  };
}

export function markIsActive(context: EditorState | Transaction, type: MarkType) {
  const { from, $from, to, empty } = context.selection;

  if (empty) {
    return type && !!type.isInSet(context.storedMarks || $from.marks());
  }

  return !!context.doc.rangeHasMark(from, to, type);
}

export function getMarkAttrs(doc: ProsemirrorNode, range: { from: number; to: number }, type: MarkType) {
  const { from, to } = range;
  let marks: Mark[] = [];

  doc.nodesBetween(from, to, node => {
    marks = [...marks, ...node.marks];
  });

  const mark = marks.find(markItem => markItem.type.name === type.name);

  if (mark) {
    return mark.attrs;
  }

  return {};
}

export function getMarkRange($pos?: ResolvedPos, type?: MarkType) {
  if (!$pos || !type) {
    return false;
  }

  const start = $pos.parent.childAfter($pos.parentOffset);

  if (!start.node) {
    return false;
  }

  const link = start.node.marks.find((mark: Mark) => mark.type === type);
  if (!link) {
    return false;
  }

  let startIndex = $pos.index();
  let startPos = $pos.start() + start.offset;
  let endIndex = startIndex + 1;
  let endPos = startPos + start.node.nodeSize;

  while (startIndex > 0 && link.isInSet($pos.parent.child(startIndex - 1).marks)) {
    startIndex -= 1;
    startPos -= $pos.parent.child(startIndex).nodeSize;
  }

  while (endIndex < $pos.parent.childCount && link.isInSet($pos.parent.child(endIndex).marks)) {
    endPos += $pos.parent.child(endIndex).nodeSize;
    endIndex += 1;
  }

  return { from: startPos, to: endPos };
}

export function getSelectionMarkRange(selection: Selection, markType: MarkType): { from: number; to: number } {
  let range: { from: number; to: number };
  if (selection.empty) {
    range = getMarkRange(selection.$head, markType) as { from: number; to: number };
  } else {
    range = { from: selection.from, to: selection.to };
  }
  return range;
}

export function removeInvalidatedMarks(
  tr: MarkTransaction,
  node: ProsemirrorNode,
  pos: number,
  re: RegExp,
  markType: MarkType,
) {
  re.lastIndex = 0;
  const markedNodes = findChildrenByMark(node, markType, true);
  markedNodes.forEach(markedNode => {
    const from = pos + 1 + markedNode.pos;
    const markedRange = getMarkRange(tr.doc.resolve(from), markType);
    if (markedRange) {
      const text = tr.doc.textBetween(markedRange.from, markedRange.to);
      if (!text.match(re)) {
        tr.removeMark(markedRange.from, markedRange.to, markType);
        tr.removeStoredMark(markType);
      }
    }
  });
  re.lastIndex = 0;
}

export function splitInvalidatedMarks(
  tr: MarkTransaction,
  node: ProsemirrorNode,
  pos: number,
  validLength: (text: string) => number,
  markType: MarkType,
  removeMark?: (from: number, to: number) => void,
) {
  const hasMarkType = (nd: ProsemirrorNode) => markType.isInSet(nd.marks);
  const markedNodes = findChildrenByMark(node, markType, true);
  markedNodes.forEach(markedNode => {
    const mark = hasMarkType(markedNode.node);
    if (mark) {
      const from = pos + 1 + markedNode.pos;
      const markRange = getMarkRange(tr.doc.resolve(from), markType);
      if (markRange) {
        const text = tr.doc.textBetween(markRange.from, markRange.to);
        const length = validLength(text);
        if (length > -1 && length !== text.length) {
          if (removeMark) {
            removeMark(markRange.from + length, markRange.to);
          } else {
            tr.removeMark(markRange.from + length, markRange.to, markType);
          }
        }
      }
    }
  });
}

export function detectAndApplyMarks(
  tr: MarkTransaction,
  node: ProsemirrorNode,
  pos: number,
  re: RegExp,
  markType: MarkType,
  attrs: (match: RegExpMatchArray) => {},
  text?: (match: RegExpMatchArray) => string,
) {
  re.lastIndex = 0;
  const textNodes = mergedTextNodes(node, (_node: ProsemirrorNode, _pos: number, parentNode: ProsemirrorNode) =>
    parentNode.type.allowsMarkType(markType),
  );
  textNodes.forEach(textNode => {
    re.lastIndex = 0;
    let match = re.exec(textNode.text);
    while (match !== null) {
      const refText = text ? text(match) : match[0];
      const from = pos + 1 + textNode.pos + match.index + (match[0].length - refText.length);
      const to = from + refText.length;
      const range = getMarkRange(tr.doc.resolve(to), markType);
      if (
        (!range || range.from !== from || range.to !== to) &&
        !tr.doc.rangeHasMark(from, to, markType.schema.marks.code)
      ) {
        const mark = markType.create(attrs instanceof Function ? attrs(match) : attrs);
        tr.addMark(from, to, mark);
        if (tr.selection.anchor === to) {
          tr.removeStoredMark(mark.type);
        }
      }
      match = re.lastIndex !== 0 ? re.exec(textNode.text) : null;
    }
  });
  re.lastIndex = 0;
}
