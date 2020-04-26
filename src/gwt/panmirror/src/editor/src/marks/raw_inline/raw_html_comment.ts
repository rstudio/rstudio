
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

import { Schema, Mark, Fragment } from "prosemirror-model";
import { Transaction, TextSelection, EditorState } from "prosemirror-state";
import { toggleMark } from "prosemirror-commands";

import { setTextSelection } from "prosemirror-utils";

import { EditorCommandId, ProsemirrorCommand } from "../../api/command";
import { canInsertNode } from "../../api/node";
import { PandocExtensions, ProsemirrorWriter, PandocOutput } from "../../api/pandoc";
import { PandocCapabilities } from "../../api/pandoc_capabilities";
import { Extension } from "../../api/extension";
import { EditorUI } from "../../api/ui";

const kHTMLCommentRegEx = /^<!--([\s\S]*?)-->\s*$/;

const extension = (pandocExtensions: PandocExtensions, pandocCapabilities: PandocCapabilities): Extension | null => {
  if (!pandocExtensions.raw_html) {
    return null;
  }

  return {
    marks: [
      {
        name: 'raw_html_comment',
        noInputRules: true,
        spec: {
          inclusive: false,
          excludes: '_',
          parseDOM: [
            {
              tag: "span[class*='raw-html-comment']",
              getAttrs(dom: Node | string) { return {}; },
            },
          ],
          toDOM(mark: Mark) {
            const attr: any = {
              class: 'raw-html-comment pm-fixedwidth-font pm-comment-color pm-comment-background-color' ,
            };
            return ['span', attr];
          },
        },
        pandoc: {
          readers: [],
          inlineHTMLReader: (schema: Schema, html: string, writer: ProsemirrorWriter) => {
            if (kHTMLCommentRegEx.test(html)) {
              const mark = schema.marks.raw_html_comment.create();
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

    // insert command
    commands: (schema: Schema, ui: EditorUI) => {
      return [
        new InsertHTMLCommentCommand(schema)
      ];
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
      if (state.doc.rangeHasMark(state.selection.to, state.selection.to+1, schema.marks.raw_html)) {
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
        const mark = schema.marks.raw_html_comment.create();
        const comment = '<!--  -->';
        const node = schema.text(comment, [mark]);
        tr.insert(tr.selection.from, node);

        // set the selection to the middle of the comment
        tr.setSelection(
          new TextSelection(tr.doc.resolve(tr.selection.from - (comment.length/2 - 1))),
        );

        // dispatch
        dispatch(tr);
      }

      return true;
    });
  }
}

export default extension;
