/*
 * raw_html-comment.ts
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

import { Schema, Mark, Fragment, Node as ProsemirrorNode } from 'prosemirror-model';
import { Transaction, TextSelection, EditorState } from 'prosemirror-state';
import { toggleMark } from 'prosemirror-commands';

import { setTextSelection } from 'prosemirror-utils';

import { EditorCommandId, ProsemirrorCommand } from '../../api/command';
import { canInsertNode } from '../../api/node';
import { PandocExtensions, ProsemirrorWriter, PandocOutput } from '../../api/pandoc';
import { Extension } from '../../api/extension';
import { EditorUI } from '../../api/ui';
import { MarkTransaction } from '../../api/transaction';
import { removeInvalidatedMarks, detectAndApplyMarks } from '../../api/mark';

const kHTMLCommentRegEx = /<!--([\s\S]*?)-->/;
const kHTMLEditingCommentRegEx = /^<!--# ([\s\S]*?)-->$/;

const extension = (): Extension | null => {
  return {
    marks: [
      {
        name: 'raw_html_comment',
        noInputRules: true,
        spec: {
          attrs: {
            editing: { default: false },
          },
          inclusive: false,
          excludes: '_',
          parseDOM: [
            {
              tag: "span[class*='raw-html-comment']",
              getAttrs(dom: Node | string) {
                const el = dom as Element;
                return {
                  editing: el.getAttribute('data-editing') === '1',
                };
              },
            },
          ],
          toDOM(mark: Mark) {
            const attr: any = {
              class:
                'raw-html-comment pm-fixedwidth-font ' +
                (mark.attrs.editing ? 'pm-comment-color pm-comment-background-color' : 'pm-light-text-color'),
            };
            return ['span', attr];
          },
        },
        pandoc: {
          readers: [],
          inlineHTMLReader: (schema: Schema, html: string, writer: ProsemirrorWriter) => {
            if (html.match(kHTMLCommentRegEx)) {
              const mark = schema.marks.raw_html_comment.create({ editing: html.match(kHTMLEditingCommentRegEx) });
              writer.openMark(mark);
              writer.writeText(html);
              writer.closeMark(mark);
              return true;
            } else {
              return false;
            }
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
      const markType = schema.marks.raw_html_comment;
      const kHTMLCommentMarkRegEx = new RegExp(kHTMLCommentRegEx.source, 'g');
      return [
        {
          name: 'html-editing-comment-marks',
          filter: (node: ProsemirrorNode) => node.isTextblock && node.type.allowsMarkType(markType),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            removeInvalidatedMarks(tr, node, pos, kHTMLCommentRegEx, markType);
            detectAndApplyMarks(tr, tr.doc.nodeAt(pos)!, pos, kHTMLCommentMarkRegEx, markType, match => ({
              editing: kHTMLEditingCommentRegEx.test(match[0]),
            }));
          },
        },
      ];
    },

    // insert command
    commands: (schema: Schema, ui: EditorUI) => {
      return [new InsertHTMLCommentCommand(schema)];
    },
  };
};

export class InsertHTMLCommentCommand extends ProsemirrorCommand {
  constructor(schema: Schema) {
    super(EditorCommandId.HTMLComment, ['Shift-Mod-c'], (state: EditorState, dispatch?: (tr: Transaction) => void) => {
      // make sure we can insert a text node here
      if (!canInsertNode(state, schema.nodes.text)) {
        return false;
      }

      // make sure we can apply this mark here
      if (!toggleMark(schema.marks.raw_html)(state)) {
        return false;
      }

      // make sure the end of the selection (where we will insert the comment)
      // isn't already in a mark of this type
      if (state.doc.rangeHasMark(state.selection.to, state.selection.to + 1, schema.marks.raw_html)) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;

        // set the selection to the end of the current selection (comment 'on' the selection)
        setTextSelection(tr.selection.to)(tr);

        // if we have a character right before us then insert a space
        const { parent, parentOffset } = tr.selection.$to;
        const charBefore = parent.textContent.slice(parentOffset - 1, parentOffset);
        if (charBefore.length && charBefore !== ' ') {
          tr.insertText(' ');
        }

        // insert the comment
        const comment = '<!--#  -->';
        const mark = schema.marks.raw_html_comment.create({ editing: true });
        tr.insert(tr.selection.to, schema.text(comment, [mark]));

        // set the selection to the middle of the comment
        tr.setSelection(new TextSelection(tr.doc.resolve(tr.selection.to - (comment.length / 2 - 1))));

        // dispatch
        dispatch(tr);
      }

      return true;
    });
  }
}

export default extension;
