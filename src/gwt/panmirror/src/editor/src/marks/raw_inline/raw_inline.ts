/*
 * raw_inline.ts
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
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { toggleMark } from 'prosemirror-commands';

import { Extension } from '../../api/extension';
import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { PandocOutput, PandocToken, PandocTokenType, PandocExtensions, ProsemirrorWriter } from '../../api/pandoc';
import { getMarkRange, markIsActive, getMarkAttrs } from '../../api/mark';
import { EditorUI, RawFormatProps } from '../../api/ui';
import { canInsertNode } from '../../api/node';

export const kRawInlineFormat = 0;
export const kRawInlineContent = 1;

const extension = (pandocExtensions: PandocExtensions): Extension | null => {
  
  if (!pandocExtensions.raw_attribute) {
    return null;
  }

  // return the extension
  return {
    marks: [
      {
        name: 'raw_inline',
        noInputRules: true,
        spec: {
          inclusive: false,
          excludes: '_',
          attrs: {
            format: {},
          },
          parseDOM: [
            {
              tag: "span[class*='raw-inline']",
              getAttrs(dom: Node | string) {
                const el = dom as Element;
                return {
                  format: el.getAttribute('data-format')
                };
              },
            },
          ],
          toDOM(mark: Mark) {
            const attr: any = {
              class: 'raw-inline pm-fixedwidth-font pm-markup-text-color',
              'data-format': mark.attrs.format,
            };
            return ['span', attr];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.RawInline,
              mark: 'raw_inline',
              getAttrs: (tok: PandocToken) => {
                return {
                  format: tok.c[kRawInlineFormat]
                };
              },
              getText: (tok: PandocToken) => {
                return tok.c[kRawInlineContent];
              }
            },
          ],
          writer: {
            priority: 20,
            write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
              // get raw content
              let raw = '';
              parent.forEach((node: ProsemirrorNode) => (raw = raw + node.textContent));
              // write it
              output.writeToken(PandocTokenType.RawInline, () => {
                output.write(mark.attrs.format);
                output.write(raw);
              });
            },
          },
        },
      },
    ],

    // insert command
    commands: (_schema: Schema, ui: EditorUI) => {
      return [
        new RawInlineCommand(ui)
      ];
    }
  };
};

class RawInlineCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI) {
    super(
      EditorCommandId.RawInline,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        if (!canInsertNode(state, schema.nodes.text) || !toggleMark(schema.marks.raw_inline)(state)) {
          return false;
        }

        async function asyncInlineRaw() {
          if (dispatch) {
            // check if mark is active
            const isActive = markIsActive(state, schema.marks.raw_inline);

            // get the range of the mark
            let range = { from: state.selection.from, to: state.selection.to };
            if (isActive) {
              range = getMarkRange(state.selection.$from, schema.marks.raw_inline) as { from: number; to: number };
            }

            // get raw attributes if we have them
            let raw: RawFormatProps = { content: '', format: '' };
            raw.content = state.doc.textBetween(range.from, range.to);
            if (isActive) {
              raw = {
                ...raw,
                ...getMarkAttrs(state.doc, state.selection, schema.marks.raw_inline),
              };
            }

            const result = await ui.dialogs.editRawInline(raw);
            if (result) {
              const tr = state.tr;
              tr.removeMark(range.from, range.to, schema.marks.raw_inline);
              if (result.action === 'edit') {
                const mark = schema.marks.raw_inline.create({ format: result.raw.format });
                const node = schema.text(result.raw.content, [mark]);
                // if we are editing a selection then replace it, otherwise insert
                if (raw.content) {
                  tr.replaceRangeWith(range.from, range.to, node);
                } else {
                  tr.replaceSelectionWith(node, false);
                }
              }
              dispatch(tr);
            }

            if (view) {
              view.focus();
            }
          }
        }
        asyncInlineRaw();

        return true;
      },
    );
  }
}

export default extension;
