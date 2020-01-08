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
            return ['pre', pandocExtensions.fenced_code_attributes ? pandocAttrToDomAttr(node.attrs) : {}, ['code', 0]];
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
