/*
 * cite.ts
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

import { Mark, Schema } from 'prosemirror-model';

import { Extension, extensionIfEnabled } from '../../api/extension';
import { EditorUI } from '../../api/ui';
import { PandocTokenType, PandocToken } from '../../api/pandoc';

import { citeAppendMarkTransaction } from './cite-transaction';
import { citeHighlightPlugin } from './cite-highlight';
import { InsertCitationCommand } from './cite-commands';
import { citePandocAstOutputFilter, citePandocWriter } from './cite-pandoc';

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
      noInputRules: true,
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
          write: citePandocWriter,
        },

        astOutputFilter: citePandocAstOutputFilter,
      },
    },
  ],

  commands: (_schema: Schema, ui: EditorUI) => {
    return [new InsertCitationCommand(ui)];
  },

  appendMarkTransaction: (_schema: Schema) => {
    return [citeAppendMarkTransaction()];
  },

  plugins: (schema: Schema) => {
    return [citeHighlightPlugin(schema)];
  },
};

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
