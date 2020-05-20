/*
 * shortcode.ts
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

import { Schema, Node as ProsemirrorNode, Mark, Fragment } from 'prosemirror-model';
import { Transaction } from 'prosemirror-state';

import { findChildren } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { PandocExtensions, PandocOutput } from '../api/pandoc';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorUI } from '../api/ui';
import { detectAndApplyMarks, removeInvalidatedMarks } from '../api/mark';
import { MarkTransaction } from '../api/transaction';
import { EditorFormat } from '../api/format';
import { FixupContext } from '../api/fixup';
import { kShortcodeRegEx } from '../api/shortcode';

const extension = (
  _exts: PandocExtensions,
  _caps: PandocCapabilities,
  _ui: EditorUI,
  format: EditorFormat,
): Extension | null => {
  if (!format.hugoExtensions.shortcodes) {
    return null;
  }

  return {
    marks: [
      {
        name: 'shortcode',
        noInputRules: true,
        spec: {
          inclusive: false,
          excludes: '_',
          attrs: {},
          parseDOM: [
            {
              tag: "span[class*='shortcode']",
            },
          ],
          toDOM(_mark: Mark) {
            return ['span', { class: 'shortcode pm-markup-text-color pm-fixedwidth-font' }];
          },
        },
        pandoc: {
          readers: [],
          writer: {
            priority: 20,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              output.writeRawMarkdown(parent);
            },
          },
        },
      },
    ],

    fixups: (schema: Schema) => {
      return [
        (tr: Transaction, context: FixupContext) => {
          if (context === FixupContext.Load) {
            // apply marks
            const markType = schema.marks.shortcode;
            const predicate = (node: ProsemirrorNode) => {
              return node.isTextblock && node.type.allowsMarkType(markType);
            };
            const markTr = new MarkTransaction(tr);
            findChildren(tr.doc, predicate).forEach(nodeWithPos => {
              const { pos } = nodeWithPos;
              detectAndCreateShortcodes(schema, markTr, pos);
            });
          }
          return tr;
        },
      ];
    },

    appendMarkTransaction: (schema: Schema) => {
      return [
        {
          name: 'shortcode-marks',
          filter: (node: ProsemirrorNode) =>
            node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.shortcode),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            removeInvalidatedMarks(tr, node, pos, kShortcodeRegEx, node.type.schema.marks.shortcode);
            detectAndCreateShortcodes(node.type.schema, tr, pos);
          },
        },
      ];
    },
  };
};

function detectAndCreateShortcodes(schema: Schema, tr: MarkTransaction, pos: number) {
  // apply marks wherever they belong
  detectAndApplyMarks(
    tr,
    tr.doc.nodeAt(pos)!,
    pos,
    kShortcodeRegEx,
    schema.marks.shortcode,
    () => ({}),
    match => match[1],
  );
}

export default extension;
