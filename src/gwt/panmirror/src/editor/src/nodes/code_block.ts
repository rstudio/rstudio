/*
 * code_block.ts
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

import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { textblockTypeInputRule } from 'prosemirror-inputrules';
import { newlineInCode, exitCode } from 'prosemirror-commands';

import { BlockCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { BaseKey } from '../api/basekeys';
import { codeNodeSpec } from '../api/code';
import { PandocOutput, PandocTokenType, PandocExtensions } from '../api/pandoc';
import { pandocAttrSpec, pandocAttrParseDom, pandocAttrToDomAttr } from '../api/pandoc_attr';

const extension = (pandocExtensions: PandocExtensions): Extension => {
  return {
    nodes: [
      {
        name: 'code_block',

        spec: {
          ...codeNodeSpec(),
          attrs: { ...(pandocExtensions.fenced_code_attributes ? pandocAttrSpec : {}) },
          parseDOM: [
            {
              tag: 'pre',
              preserveWhitespace: 'full',
              getAttrs: (node: Node | string) => {
                if (pandocExtensions.fenced_code_attributes) {
                  const el = node as Element;
                  return pandocAttrParseDom(el, {});
                } else {
                  return {};
                }
              },
            },
          ],
          toDOM(node: ProsemirrorNode) {
            const fontClass = 'pm-fixedwidth-font';
            const attrs = pandocExtensions.fenced_code_attributes ?
              pandocAttrToDomAttr({
                ...node.attrs,
                classes: [...node.attrs.classes, fontClass],
              }) :
              {
                class: fontClass
              };
            return ['pre', attrs, ['code', 0]];
          },
        },

        code_view: {
          lang: (node: ProsemirrorNode) => {
            if (node.attrs.classes && node.attrs.classes.length) {
              return node.attrs.classes[0];
            } else {
              return null;
            }
          },
        },

        pandoc: {
          readers: [
            {
              token: PandocTokenType.CodeBlock,
              code_block: true,
            },
          ],
          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.CodeBlock, () => {
              if (pandocExtensions.fenced_code_attributes) {
                output.writeAttr(node.attrs.id, node.attrs.classes, node.attrs.keyvalue);
              } else {
                output.writeAttr();
              }
              output.write(node.textContent);
            });
          },
        },
      },
    ],

    commands: (schema: Schema) => {
      return [
        new BlockCommand(
          EditorCommandId.CodeBlock,
          ['Shift-Ctrl-\\'],
          schema.nodes.code_block,
          schema.nodes.paragraph,
          {},
        ),
      ];
    },

    baseKeys: () => {
      return [
        { key: BaseKey.Enter, command: newlineInCode },
        { key: BaseKey.ModEnter, command: exitCode },
        { key: BaseKey.ShiftEnter, command: exitCode },
      ];
    },

    inputRules: (schema: Schema) => {
      return [textblockTypeInputRule(/^```/, schema.nodes.code_block)];
    },
  };
};

export default extension;
