/*
 * smarty.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import { ellipsis, InputRule } from 'prosemirror-inputrules';
import { Plugin, PluginKey, EditorState, Transaction, TextSelection } from 'prosemirror-state';
import { Schema } from 'prosemirror-model';

import { Extension, extensionIfEnabled } from '../api/extension';
import { PandocMark } from '../api/mark';
import { Step, AddMarkStep } from 'prosemirror-transform';
import { fancyQuotesToSimple } from '../api/quote';

const plugin = new PluginKey('smartypaste');

// match enDash but only for lines that aren't an html comment
const enDash = new InputRule(/[^!-]--$/, (state: EditorState, match: string[], start: number, end: number) => {
  const { parent, parentOffset } = state.selection.$head;
  const precedingText = parent.textBetween(0, parentOffset);
  if (precedingText.indexOf('<!--') === -1) {
    const tr = state.tr;
    tr.insertText('–', end - 1, end);
    return tr;
  } else {
    return null;
  }
});

const emDash = new InputRule(/–-$/, (state: EditorState, match: string[], start: number, end: number) => {
  const tr = state.tr;
  tr.insertText('—', end - 1, end);
  return tr;
});

// from: https://github.com/ProseMirror/prosemirror-inputrules/blob/master/src/rules.js
// (forked so we could customize/override default behavior behavior)
const openDoubleQuote = new InputRule(/(?:^|[\s`\*_=\{\[\(\<'"\u2018\u201C])(")$/, "“");
const closeDoubleQuote = new InputRule(/"$/, "”");
const openSingleQuote = new InputRule(/(?:^|[\s`\*_=\{\[\(\<'"\u2018\u201C])(')$/, "‘");
const closeSingleQuote = new InputRule(/'$/, "’");

const extension: Extension = {
  inputRules: () => {
    return [...[openDoubleQuote, closeDoubleQuote, openSingleQuote, closeSingleQuote], ellipsis, enDash, emDash];
  },

  plugins: (schema: Schema) => {
    return [
      // apply smarty rules to plain text pastes
      new Plugin({
        key: plugin,
        props: {
          transformPastedText(text: string) {
            // double quotes
            text = text.replace(/(?:^|[\s{[(<'"\u2018\u201C])(")/g, x => {
              return x.slice(0, x.length - 1) + '“';
            });
            text = text.replace(/"/g, '”');

            // single quotes
            text = text.replace(/(?:^|[\s{[(<'"\u2018\u201C])(')/g, x => {
              return x.slice(0, x.length - 1) + '‘';
            });
            text = text.replace(/'/g, '’');

            // emdash
            text = text.replace(/(\w)---(\w)/g, '$1—$2');

            // endash
            text = text.replace(/(\w)--(\w)/g, '$1–$2');

            // ellipses
            text = text.replace(/\.\.\./g, '…');

            return text;
          },
        },
      }),
    ];
  },
};

export function reverseSmartQuotesExtension(marks: readonly PandocMark[]) {
  
  return {

    appendTransaction: (schema: Schema) => {
  
      const noInputRuleMarks = marks.filter(mark => mark.noInputRules).map(mark => schema.marks[mark.name]);

      // detect add code steps
      const isAddMarkWithNoInputRules = (step: Step) => {
        return step instanceof AddMarkStep && noInputRuleMarks.includes((step as any).mark.type);
      };

      return [
        {
          name: 'reverse-smarty',
          filter: (transactions: Transaction[]) => transactions.some(transaction => transaction.steps.some(isAddMarkWithNoInputRules)),
          append: (tr: Transaction, transactions: Transaction[]) => {
            transactions.forEach(transaction => {
              transaction.steps.filter(isAddMarkWithNoInputRules).forEach(step => {
                const { from, to, mark } = step as any;
                const code = tr.doc.textBetween(from, to);
                const newCode = fancyQuotesToSimple(code);
                if (newCode !== code) {
                  // track selection for restore
                  const prevSelection = tr.selection;

                  // determine  marks to apply
                  const $from = tr.doc.resolve(from);
                  const rangeMarks = $from.marksAcross(tr.doc.resolve(to)) || [];
                  if (!rangeMarks.find(rangeMark => rangeMark.type)) {
                    rangeMarks.push(mark);
                  }
                  
                  // replace 
                  tr.replaceRangeWith(from, to, schema.text(newCode, rangeMarks));
                  
                  // restore selection
                  if (prevSelection.empty) {
                    tr.setSelection(new TextSelection(tr.doc.resolve(prevSelection.anchor)));
                  }

                  // clear stored marks
                  if (tr.selection.empty) {
                    tr.setStoredMarks([]);
                  }
                }
              });
            });
          },
        },
      ];
    },

  };

}

export default extensionIfEnabled(extension, 'smart');
