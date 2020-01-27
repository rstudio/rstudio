/*
 * code.ts
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import { Fragment, Mark, Node as ProsemirrorNode, Schema } from 'prosemirror-model';

import { MarkCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { pandocAttrSpec, pandocAttrParseDom, pandocAttrToDomAttr, pandocAttrReadAST } from '../api/pandoc_attr';
import { PandocToken, PandocOutput, PandocTokenType, PandocExtensions } from '../api/pandoc';
import { delimiterMarkInputRule } from '../api/mark';

const CODE_ATTR = 0;
const CODE_TEXT = 1;

const extension = (pandocExtensions: PandocExtensions): Extension => {
  const codeAttrs = pandocExtensions.inline_code_attributes;

  return {
    marks: [
      {
        name: 'code',
        noInputRules: true,
        spec: {
          attrs: codeAttrs ? pandocAttrSpec : {},
          parseDOM: [
            {
              tag: 'code',
              getAttrs(dom: Node | string) {
                if (codeAttrs) {
                  return pandocAttrParseDom(dom as Element, {});
                } else {
                  return {};
                }
              },
            },
          ],
          toDOM(mark: Mark) {
            return ['code', codeAttrs ? pandocAttrToDomAttr(mark.attrs) : {}];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.Code,
              mark: 'code',
              getText: (tok: PandocToken) => tok.c[CODE_TEXT],
              getAttrs: (tok: PandocToken) => {
                if (codeAttrs) {
                  return pandocAttrReadAST(tok, CODE_ATTR);
                } else {
                  return {};
                }
              },
            },
          ],
          writer: {
            priority: 20,
            write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
              output.writeToken(PandocTokenType.Code, () => {
                if (codeAttrs) {
                  output.writeAttr(mark.attrs.id, mark.attrs.classes, mark.attrs.keyvalue);
                } else {
                  output.writeAttr();
                }
                let code = '';
                parent.forEach((node: ProsemirrorNode) => (code = code + node.textContent));
                output.write(code);
              });
            },
          },
        },
      },
    ],

    commands: (schema: Schema) => {
      return [new MarkCommand(EditorCommandId.Code, ['Mod-d'], schema.marks.code)];
    },

    inputRules: (schema: Schema) => {
      return [delimiterMarkInputRule('`', schema.marks.code)];
    },
  };
};

export default extension;
