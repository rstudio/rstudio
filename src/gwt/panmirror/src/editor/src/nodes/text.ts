/*
 * text.ts
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

import { Node as ProsemirrorNode } from 'prosemirror-model';

import { PandocOutput, PandocToken, PandocTokenType, ProsemirrorWriter } from '../api/pandoc';
import { ExtensionContext } from '../api/extension';
import { kQuoteType, QuoteType, kQuoteChildren, fancyQuotesToSimple } from '../api/quote';

const extension = (context: ExtensionContext) => {

  const readText = (text: string) => {

    // we explicitly don't want fancy quotes in the editor
    text = fancyQuotesToSimple(text);

    if (context.pandocExtensions.smart) {
      return text
        .replace(/---/g, '—')
        .replace(/--/g, '–')
        .replace(/\.\.\./g, '…');
    } else {
      return text;
    }
  };

  return {
    nodes: [
      {
        name: 'text',
        spec: {
          group: 'inline',
          toDOM(node: ProsemirrorNode): any {
            return node.text;
          },
        },
        pandoc: {
          readers: [
            { token: PandocTokenType.Str, text: true, getText: (t: PandocToken) => readText(t.c) },
            { token: PandocTokenType.Space, text: true, getText: () => ' ' },
            { token: PandocTokenType.SoftBreak, text: true, getText: () => ' ' },
            {
              token: PandocTokenType.Quoted,
              handler: () => (writer: ProsemirrorWriter, tok: PandocToken) => {
                const type = tok.c[kQuoteType].t;
                const quote = type === QuoteType.SingleQuote ? '\'' : '"';
                writer.writeTokens([
                  { t: 'Str', c: quote },
                  ...tok.c[kQuoteChildren],
                  { t: 'Str', c: quote },
                ]);
              }
            },
          ],
          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            const text = node.textContent;
            output.writeText(text);
          },
        },
      },
    ],
  };
};

export default extension;
