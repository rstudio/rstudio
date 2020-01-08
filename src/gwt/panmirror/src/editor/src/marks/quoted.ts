import { Schema, Mark, Fragment, Node as ProsemirrorNode } from 'prosemirror-model';

import { Extension } from '../api/extension';
import { PandocOutput, PandocToken, PandocTokenType } from '../api/pandoc';
import { removeInvalidatedMarks, detectAndApplyMarks } from '../api/mark';
import { MarkTransaction } from '../api/transaction';

const QUOTE_TYPE = 0;
const QUOTED_CHILDREN = 1;

const kDoubleQuoted = /“[^”]*”/;
const kSingleQuoted = /‘[^’]*’/;
const kQuoted = /(“[^”]*”|‘[^’]*’)/;

enum QuoteType {
  SingleQuote = 'SingleQuote',
  DoubleQuote = 'DoubleQuote',
}

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
          priority: 3,
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

  appendMarkTransaction: (_schema: Schema) => {
    return [
      {
        name: 'quoted-marks',

        filter: node => node.isTextblock && node.type.allowsMarkType(node.type.schema.marks.quoted),

        append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
          const schema = node.type.schema;

          // find quoted marks where the text is no longer quoted (remove the mark)
          removeInvalidatedMarks(tr, node, pos, kQuoted, schema.marks.quoted);

          // find quoted text that doesn't have a quoted mark (add the mark)
          detectAndApplyMarks(tr, tr.doc.nodeAt(pos)!, pos, kDoubleQuoted, schema.marks.quoted, {
            type: QuoteType.DoubleQuote,
          });
          detectAndApplyMarks(tr, tr.doc.nodeAt(pos)!, pos, kSingleQuoted, schema.marks.quoted, {
            type: QuoteType.SingleQuote,
          });
        },
      },
    ];
  },
};

function quotesForType(type: QuoteType) {
  const dblQuote = type === QuoteType.DoubleQuote;
  return {
    begin: dblQuote ? '“' : '‘',
    end: dblQuote ? '”' : '’',
  };
}

export default extension;
