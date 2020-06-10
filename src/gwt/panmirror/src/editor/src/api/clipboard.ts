/*
 * clipboard.ts
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

import { Slice, Fragment, MarkType, Node as ProsemirrorNode } from 'prosemirror-model';

// add marks to plain text pasted into the editor (e.g. urls become links)
// https://github.com/ProseMirror/prosemirror/issues/90
export function markPasteHandler(regexp: RegExp, type: MarkType, getAttrs: (s: string) => {}) {
  const handler = (fragment: Fragment) => {
    regexp.lastIndex = 0;

    const nodes: ProsemirrorNode[] = [];

    fragment.forEach((child: ProsemirrorNode) => {
      if (child.isText) {
        const { text } = child;
        let pos = 0;
        let match;

        do {
          match = regexp.exec(text!);
          if (match) {
            const start = match.index;
            const end = start + match[0].length;
            const attrs = getAttrs instanceof Function ? getAttrs(match[0]) : getAttrs;

            if (start > 0) {
              nodes.push(child.cut(pos, start));
            }

            nodes.push(child.cut(start, end).mark(type.create(attrs).addToSet(child.marks)));

            pos = end;
          }
        } while (match);

        if (pos < text!.length) {
          nodes.push(child.cut(pos));
        }
      } else {
        nodes.push(child.copy(handler(child.content)));
      }
    });

    regexp.lastIndex = 0;

    return Fragment.fromArray(nodes);
  };

  return (slice: Slice) => new Slice(handler(slice.content), slice.openStart, slice.openEnd);
}
