/*
 * cite.ts
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

import { Mark, Schema, Fragment, Node as ProsemirrorNode } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { EditorState, TextSelection } from 'prosemirror-state';

import { Extension, extensionIfEnabled } from '../../api/extension';
import { EditorUI } from '../../api/ui';
import { PandocTokenType, PandocToken, PandocOutput } from '../../api/pandoc';
import { fragmentText } from '../../api/fragment';

import { citeHighlightPlugin } from './cite-highlight';
import { InsertCitationCommand } from './cite-commands';

const CITE_CITATIONS = 0;

enum CitationMode {
  NormalCitation = 'NormalCitation',
  AuthorInText = 'AuthorInText',
  SuppressAuthor = 'SuppressAuthor',
}

interface Citation {
  citationHash: number;
  citationId: string;
  citationMode: {
    t: CitationMode;
  };
  citationNoteNum: number;
  citationPrefix: PandocToken[];
  citationSuffix: PandocToken[];
}

const extension: Extension = {
  marks: [
    {
      name: 'cite',
      spec: {
        attrs: {},
        inclusive: false,
        parseDOM: [
          {
            tag: "span[class*='cite']",
          },
        ],
        toDOM(mark: Mark) {
          return ['span', { class: 'cite' }];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Cite,
            mark: 'cite',
            getChildren: (tok: PandocToken) => {
              return citationsTokens(tok.c[CITE_CITATIONS]);
            },
          },
        ],

        writer: {
          priority: 14,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {

            // TODO: @id should be it's own mark type created via input rule
            // TODO: semicolon escaping is still a thing....

            // divide out delimiters from body
            const openCite = parent.cut(0, 1);
            const cite = parent.cut(1, parent.size - 1);
            const closeCite = parent.cut(parent.size - 1, parent.size);

            // proceed if the citation is still valid
            const kCiteRe = /(.* -?@|-?@)[\w:.#$%&-+?<>~/]+.*/;
            if (fragmentText(openCite) === '[' && 
                fragmentText(closeCite) === ']'&&
                kCiteRe.test(fragmentText(cite))) {
              output.writeRawMarkdown('[');
              output.withOption('citationEscaping', true, () => {
                output.writeInlines(cite);
              });
              output.writeRawMarkdown(']');
            } else {
              output.writeInlines(parent);
            }
          },
        },
      },
    },
  ],

  commands: (_schema: Schema, ui: EditorUI) => {
    return [new InsertCitationCommand(ui)];
  },

  inputRules: (schema: Schema) => {
    return [citeInputRule(schema)];
  },

  plugins: (schema: Schema) => {
    return [citeHighlightPlugin(schema)];
  },
};

function citeInputRule(schema: Schema) {
  return new InputRule(/@$/, (state: EditorState, match: string[], start: number, end: number) => {
    // check that the @ sign is enclosed between brackets
    const prevChar = state.doc.textBetween(start - 1, start);
    const nextChar = state.doc.textBetween(end, end + 1);
    if (prevChar === '[' && nextChar === ']') {
      const tr = state.tr;
      const cite = '@cite';
      tr.insertText(cite);
      tr.setSelection(new TextSelection(
        tr.doc.resolve(start + 1), tr.doc.resolve(start + cite.length)
      ));
      const mark = schema.marks.cite.create();
      tr.addMark(start - 1, start + cite.length + 1, mark);
      return tr;

    } else {
      return null;
    }
  });
}

function citationsTokens(citations: Citation[]) {
  const tokens: PandocToken[] = [];

  tokens.push({ t: PandocTokenType.Str, c: '[' });

  citations.forEach((citation: Citation, index: number) => {
    // add delimiter
    if (index !== 0) {
      tokens.push({ t: PandocTokenType.Str, c: ';' }, { t: PandocTokenType.Space });
    }

    // suppress author?
    const suppress = citation.citationMode.t === CitationMode.SuppressAuthor ? '-' : '';

    // citation prefex, id, and suffix
    tokens.push(...citation.citationPrefix);
    if (citation.citationPrefix.length) {
      tokens.push({ t: PandocTokenType.Space });
    }
    tokens.push({ t: PandocTokenType.Str, c: suppress + '@' + citation.citationId });
    if (citation.citationMode.t === CitationMode.AuthorInText && citation.citationSuffix.length) {
      tokens.push({ t: PandocTokenType.Space });
    }
    tokens.push(...citation.citationSuffix);
  });

  tokens.push({ t: PandocTokenType.Str, c: ']' });

  return tokens;
}

export default extensionIfEnabled(extension, 'citations');
