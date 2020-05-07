/*
 * pandoc_block_capsule.ts
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

import { PandocBlockCapsuleFilter, PandocToken, PandocTokenType, mapTokens, PandocBlockCapsule } from "../api/pandoc";

// constants used for creating/consuming capsules
const kFieldDelimiter = '\n';
const kValueDelimiter = ':';
const kTypeField = 'type';
const kPrefixField = 'prefix';
const kSourceField = 'source';
const kSuffixField = 'suffix';
const kBlockCapsuleSentinel = '31B8E172-B470-440E-83D8-E6B185028602'.toLowerCase();


// transform the passed markdown to include base64 encoded block capsules as specified by the
// provided capsule filter. capsules are used to hoist block types that we don't want pandoc
// to see (e.g. yaml metadata or Rmd chunks) out of the markdown, only to be re-inserted
// after pandoc has yielded an ast. block capsules are simple paragraph text, so that they can
// both go wherever a block can go, and also so that they can be embedded inside all other
// block types (i.e. if we created the capsule as a backtick code block then we couldn't embed
// it within another code block w/o worrying about backtick escaping rules for the parent)
export function pandocMarkdownWithBlockCapsules(markdown: string, capsuleFilter: PandocBlockCapsuleFilter) {

  // replace all w/ source preservation capsules
  return markdown.replace(capsuleFilter.match, (_match: string, p1: string, p2: string, p3: string) => {

    // make the capsule
    const capsule : PandocBlockCapsule = {
      [kTypeField]: capsuleFilter.type,
      [kPrefixField]: p1,
      [kSourceField]: p2,
      [kSuffixField]: p3
    };

    // constuct a field
    const field = (name: string, value: string) => `${name}${kValueDelimiter}${btoa(value)}`;

    // construct a record 
    const record =
       field(kTypeField, capsule.type) + kFieldDelimiter +
       field(kPrefixField, capsule.prefix) + kFieldDelimiter +
       field(kSourceField, capsule.source) + kFieldDelimiter +
       field(kSuffixField, capsule.suffix);
   
    // now base64 encode the entire record (so it can masquerade as a paragraph)
    const encodedRecord = btoa(record);

    // return capsule (sentinel followed by the encoded record) with delimiters, surrounded by prefix and suffix
    return p1 +  capsuleFilter.enclose(`${kBlockCapsuleSentinel}${kValueDelimiter}${encodedRecord}${kValueDelimiter}${kBlockCapsuleSentinel}`, capsule) + p3;

  });
  
}


// block capsules can also end up not as block tokens, but rather as text within another
// token (e.g. within a backtick code block or raw_block). this function takes a set
// of pandoc tokens and recursively converts block capsules that aren't of type
// PandocTokenType.Str (which is what we'd see in a paragraph) into their original form
export function resolvePandocBlockCapsuleText(tokens: PandocToken[], filter: PandocBlockCapsuleFilter) : PandocToken[] {
 
  // process all tokens
  return mapTokens(tokens, token => {

    // look for non pandoc string content
    if (token.c) {
      // otherwise see if there is block capsule text to convert within this token
      if (typeof token.c === "string") {
        token.c = filter.handleText(token.c);
      } else if (Array.isArray(token.c)) {
        const children = token.c.length;
        for (let i=0; i<children; i++) {
          if (typeof token.c[i] === "string") {
            token.c[i] = filter.handleText(token.c[i]);
          }
        }
      }
    }

    return token;
  });

}

