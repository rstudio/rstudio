/*
 * code.ts
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

import { Fragment, Mark, Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { Step, AddMarkStep } from 'prosemirror-transform';
import { Transaction } from 'prosemirror-state';

import { MarkCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { pandocAttrSpec, pandocAttrParseDom, pandocAttrToDomAttr, pandocAttrReadAST } from '../api/pandoc_attr';
import { PandocToken, PandocOutput, PandocTokenType, PandocExtensions } from '../api/pandoc';

import { fancyQuotesToSimple } from '../api/quote';
import { kCodeText, kCodeAttr } from '../api/code';
import { delimiterMarkInputRule, MarkInputRuleFilter } from '../api/input_rule';


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
            const fontClass = 'pm-code pm-fixedwidth-font pm-chunk-background-color pm-block-border-color';
            const attrs = codeAttrs
              ? pandocAttrToDomAttr({
                  ...mark.attrs,
                  classes: [...mark.attrs.classes, fontClass],
                })
              : {
                  class: fontClass,
                };
            return ['code', attrs];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.Code,
              mark: 'code',
              getText: (tok: PandocToken) => tok.c[kCodeText],
              getAttrs: (tok: PandocToken) => {
                if (codeAttrs) {
                  return pandocAttrReadAST(tok, kCodeAttr);
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

    inputRules: (schema: Schema, filter: MarkInputRuleFilter) => {
      return [delimiterMarkInputRule('`', schema.marks.code, filter)];
    },

    appendTransaction: (schema: Schema) => {
  
       // detect add code steps
      const isAddCodeMarkStep = (step: Step) => {
        return step instanceof AddMarkStep && (step as any).mark.type === schema.marks.code;
      };

      return [
        {
          name: 'code_remove_quotes',
          filter: (transactions: Transaction[]) => transactions.some(transaction => transaction.steps.some(isAddCodeMarkStep)),
          append: (tr: Transaction, transactions: Transaction[]) => {
            transactions.forEach(transaction => {
              transaction.steps.filter(isAddCodeMarkStep).forEach(step => {
                const { from, to } = step as any;
                const code = tr.doc.textBetween(from, to);
                const newCode = fancyQuotesToSimple(code);
                if (newCode !== code) {
                  tr.replaceWith(from, to, schema.text(newCode, [schema.marks.code.create()]));
                  tr.removeStoredMark(schema.marks.code);
                }
              });
            });
          },
        },
      ];
    },
  };
};

export default extension;
