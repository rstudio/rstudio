/*
 * link-auto.ts
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

import { Schema } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { EditorState } from 'prosemirror-state';
import { setTextSelection } from 'prosemirror-utils';

import { markInputRule } from '../../api/mark';
import { markPasteHandler } from '../../api/clipboard';
import { EditorOutlineItem } from '../../api/outline';

import { getOutline } from '../../behaviors/outline';

export function linkInputRules(autoLink: boolean, headingLink: boolean) {
  return (schema: Schema) => {
    const rules = [
      // <link> style link
      markInputRule(/(?:<)([a-z]+:\/\/[^>]+)(?:>)$/, schema.marks.link, (match: string[]) => ({ href: match[1] })),
      // full markdown link
      markInputRule(/(?:\[)([^\]]+)(?:\]\()([^)]+)(?:\))$/, schema.marks.link, (match: string[]) => ({
        href: match[2],
      })),
    ];

    if (autoLink) {
      // plain link
      rules.push(
        new InputRule(/([a-z]+:\/\/[^\s]+) $/, (state: EditorState, match: string[], start: number, end: number) => {
          const tr = state.tr;
          end = start + match[1].length;
          tr.addMark(start, end, schema.marks.link.create({ href: match[1] }));
          tr.removeStoredMark(schema.marks.link);
          tr.insertText(' ');
          setTextSelection(end + 1)(tr);
          return tr;
        }),
      );
    }

    if (headingLink) {
      rules.push(
        new InputRule(/(?:\[)([^@]+)(?:\] )/, (state: EditorState, match: string[], start: number, end: number) => {
          // check to see if the text matches a heading
          const text = match[1];
          const outline = getOutline(state);
          const hasMatchingHeading = (item: EditorOutlineItem) => {
            if (item.type === 'heading' && item.title === text) {
              return true;
            } else {
              return item.children.some(hasMatchingHeading);
            }
          };
          if (outline.some(hasMatchingHeading)) {
            const tr = state.tr;
            tr.addMark(
              start + 1,
              start + 1 + text.length,
              state.schema.marks.link.create({ href: text, heading: text }),
            );
            tr.delete(start, start + 1);
            tr.delete(end - 2, end - 1);
            tr.insertText(' ');
            return tr;
          } else {
            return null;
          }
        }),
      );
    }

    return rules;
  };
}

export function linkPasteHandler(schema: Schema) {
  return markPasteHandler(/[a-z]+:\/\/[^\s]+/g, schema.marks.link, url => ({ href: url }));
}
