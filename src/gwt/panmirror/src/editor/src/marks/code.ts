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
