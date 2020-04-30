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
import { Transaction, EditorState } from 'prosemirror-state';
import { toggleMark } from 'prosemirror-commands';

import { setTextSelection, findChildren, findChildrenByMark } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { PandocExtensions, PandocOutput } from '../api/pandoc';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorUI } from '../api/ui';
import { detectAndApplyMarks, removeInvalidatedMarks, getMarkRange } from '../api/mark';
import { MarkTransaction } from '../api/transaction';
import { EditorFormat, kBlogdownDocType } from '../api/format';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { canInsertNode } from '../api/node';
import { FixupContext } from '../api/fixup';
import { quotesForType, QuoteType } from '../api/quote';

const kShortcodePattern = '{{([%<])\\s+.*?[%>]}}';
const kShortcodeRegEx = new RegExp(kShortcodePattern, 'g');

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

    commands: (schema: Schema) => {
      // only create command for blogdown docs
      if (!format.docTypes.includes(kBlogdownDocType)) {
        return [];
      }

      return [
        new ProsemirrorCommand(
          EditorCommandId.Shortcode,
          [],
          (state: EditorState, dispatch?: (tr: Transaction<any>) => void) => {
            // enable/disable command
            if (!canInsertNode(state, schema.nodes.text) || !toggleMark(schema.marks.shortcode)(state)) {
              return false;
            }
            if (dispatch) {
              const tr = state.tr;
              const selection = tr.selection;
              const shortcode = '{{<  >}}';
              tr.replaceSelectionWith(schema.text(shortcode));
              setTextSelection(tr.mapping.map(selection.head) - shortcode.length / 2)(tr);
              dispatch(tr);
            }
            return true;
          },
        ),
      ];
    },
  };
};

function detectAndCreateShortcodes(schema: Schema, tr: MarkTransaction, pos: number) {
  // create regexs for removing quotes
  const singleQuote = quotesForType(QuoteType.SingleQuote);
  const singleQuoteRegEx = new RegExp(`[${singleQuote.begin}${singleQuote.end}]`, 'g');
  const doubleQuote = quotesForType(QuoteType.DoubleQuote);
  const doubleQuoteRegEx = new RegExp(`[${doubleQuote.begin}${doubleQuote.end}]`, 'g');

  // apply marks wherever they belong
  detectAndApplyMarks(tr, tr.doc.nodeAt(pos)!, pos, kShortcodeRegEx, schema.marks.shortcode);

  // remove quotes as necessary
  const markType = schema.marks.shortcode;
  const markedNodes = findChildrenByMark(tr.doc.nodeAt(pos)!, markType, true);
  markedNodes.forEach(markedNode => {
    const from = pos + 1 + markedNode.pos;
    const markedRange = getMarkRange(tr.doc.resolve(from), markType);
    if (markedRange) {
      const text = tr.doc.textBetween(markedRange.from, markedRange.to);
      const replaceText = text.replace(singleQuoteRegEx, "'").replace(doubleQuoteRegEx, '"');
      if (replaceText !== text) {
        tr.insertText(replaceText, markedRange.from);
      }
    }
  });
}

export default extension;
