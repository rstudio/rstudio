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

import { setTextSelection } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { PandocExtensions, PandocOutput, ProsemirrorWriter, PandocTokenType, PandocToken } from '../api/pandoc';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorUI } from '../api/ui';
import { detectAndApplyMarks, removeInvalidatedMarks } from '../api/mark';
import { MarkTransaction } from '../api/transaction';
import { EditorFormat } from '../api/format';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { canInsertNode } from '../api/node';
import { kRawInlineFormat, kRawInlineContent } from './raw_inline/raw_inline';

const kShortcodePattern = "{{([%<])\\s+.*?[%>]}}";
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
          readers: [
            {
              token: PandocTokenType.RawInline,
              match: (tok: PandocToken) => {
                const rawFormat = tok.c[kRawInlineFormat];
                return rawFormat === 'shortcode';
              },
              handler: (schema: Schema) => {
                return (writer: ProsemirrorWriter, tok: PandocToken) => {
                  const shortcode = tok.c[kRawInlineContent];
                  const mark = schema.marks.shortcode.create();
                  writer.openMark(mark);
                  writer.writeText(shortcode);
                  writer.closeMark(mark);
                };
              },
            },
          ],
          preprocessor: (markdown: string) => {
            const md = markdown.replace(
              kShortcodeRegEx,
              (match: string) => {
                // use double-backtick w/ space to allow for ` inside the match, see
                // https://meta.stackexchange.com/questions/82718/how-do-i-escape-a-backtick-within-in-line-code-in-markdown
                return '`` ' + match + ' ``{=shortcode}';
              },
            );
            return md;
          },
          writer: {
            priority: 20,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              output.writeRawMarkdown(parent);
            },
          },
        },
      },
    ],

    appendMarkTransaction: (schema: Schema) => {
      return [
        {
          name: 'shortcode-marks',
          filter: (node: ProsemirrorNode) => node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.shortcode),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            removeInvalidatedMarks(tr, node, pos, kShortcodeRegEx, node.type.schema.marks.shortcode);
            detectAndApplyMarks(tr, tr.doc.nodeAt(pos)!, pos, kShortcodeRegEx, node.type.schema.marks.shortcode);
          },
        },
      ];
    },

    commands: (schema: Schema) => {
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
              setTextSelection(tr.mapping.map(selection.head) - (shortcode.length/2))(tr);
              dispatch(tr);
            }
            return true;
          },
        ),
      ];
    },
  };
};

export default extension;
