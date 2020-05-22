/*
 * quote.ts
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

export const kQuoteType = 0;
export const kQuoteChildren = 1;

export enum QuoteType {
  SingleQuote = 'SingleQuote',
  DoubleQuote = 'DoubleQuote',
}

export function quotesForType(type: QuoteType) {
  const dblQuote = type === QuoteType.DoubleQuote;
  return {
    begin: dblQuote ? '“' : '‘',
    end: dblQuote ? '”' : '’',
  };
}

// create regexs for removing quotes
const kSingleQuotes = quotesForType(QuoteType.SingleQuote);
const kSingleQuoteRegEx = new RegExp(`[${kSingleQuotes.begin}${kSingleQuotes.end}]`, 'g');
const kDoubleQuotes = quotesForType(QuoteType.DoubleQuote);
const kDoubleQuoteRegEx = new RegExp(`[${kDoubleQuotes.begin}${kDoubleQuotes.end}]`, 'g');

export function fancyQuotesToSimple(text: string) {
  return text.replace(kSingleQuoteRegEx, "'").replace(kDoubleQuoteRegEx, '"');
}
