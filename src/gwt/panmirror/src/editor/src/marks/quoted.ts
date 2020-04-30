/*
 * quoted.ts
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
import { Transaction } from 'prosemirror-state';
import { findChildren } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { PandocOutput, PandocToken, PandocTokenType } from '../api/pandoc';
import { removeInvalidatedMarks, detectAndApplyMarks } from '../api/mark';
import { FixupContext } from '../api/fixup';
import { MarkTransaction } from '../api/transaction';
import { QuoteType, quotesForType } from '../api/quote';

const QUOTE_TYPE = 0;
const QUOTED_CHILDREN = 1;

const extension: Extension = {
  marks: [
    {
      name: 'quoted',
      spec: {
        inclusive: false,
        attrs: {
          type: {},
        },
        parseDOM: [
          {
            tag: "span[class*='quoted']",
            getAttrs(dom: Node | string) {
              const el = dom as Element;
              return {
                type: el.getAttribute('data-type'),
              };
            },
          },
        ],
        toDOM(mark: Mark) {
          return ['span', { class: 'quoted', 'data-type': mark.attrs.type }];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Quoted,
            mark: 'quoted',
            getAttrs: (tok: PandocToken) => {
              return {
                type: tok.c[QUOTE_TYPE].t,
              };
            },
            getChildren: (tok: PandocToken) => {
              const type = tok.c[QUOTE_TYPE].t;
              const quotes = quotesForType(type);
              return [
                {
                  t: 'Str',
                  c: quotes.begin,
                },
                ...tok.c[QUOTED_CHILDREN],
                {
                  t: 'Str',
                  c: quotes.end,
                },
              ];
            },
          },
        ],
        writer: {
          priority: 17,
          write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
            output.writeToken(PandocTokenType.Quoted, () => {
              output.writeToken(mark.attrs.type);
              output.writeArray(() => {
                const text = parent.cut(1, parent.size - 1);
                output.writeInlines(text);
              });
            });
          },
        },
      },
    },
  ],

  fixups: (schema: Schema) => {
    return [
      (tr: Transaction, context: FixupContext) => {
        // only apply on save
        if (context !== FixupContext.Save) {
          return tr;
        }

        // create mark transation wrapper
        const markTr = new MarkTransaction(tr);

        const predicate = (node: ProsemirrorNode) => {
          return node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.quoted);
        };
        findChildren(tr.doc, predicate).forEach(nodeWithPos => {
          const { node, pos } = nodeWithPos;

          // find quoted marks where the text is no longer quoted (remove the mark)
          removeInvalidatedMarks(markTr, node, pos, /(“[^”]*”|‘[^’]*’)/g, schema.marks.quoted);

          // find quoted text that doesn't have a quoted mark (add the mark)
          detectAndApplyMarks(markTr, tr.doc.nodeAt(pos)!, pos, /“[^”]*”/g, schema.marks.quoted, {
            type: QuoteType.DoubleQuote,
          });
          detectAndApplyMarks(markTr, tr.doc.nodeAt(pos)!, pos, /‘[^’]*’/g, schema.marks.quoted, {
            type: QuoteType.SingleQuote,
          });
        });

        return tr;
      },
    ];
  },
};

export default extension;
