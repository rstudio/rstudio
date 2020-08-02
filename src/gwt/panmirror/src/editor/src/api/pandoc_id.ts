/*
 * pandoc_id.ts
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

import { emojis, SkinTone, emojiForAllSkinTones } from "./emoji";

// emulate pandoc behavior (https://pandoc.org/MANUAL.html#headings-and-sections)
export function pandocAutoIdentifier(text: string) {
  return text
    // Remove all non-alphanumeric characters, except underscores, hyphens, and periods.
    .replace(/[^ _\-.\w]+/g, '')

    // Replace all spaces and newlines with hyphens
    .replace(/[ \n]/g, '-')

    // Convert all alphabetic characters to lowercase
    .toLowerCase()

    // Remove everything up to the first letter 
    // (identifiers may not begin with a number or punctuation mark
    .replace(/^[^A-Za-z]+/, '');
}

// emulate github behavior: 
// https://pandoc.org/MANUAL.html#extension-gfm_auto_identifiers
export function gfmAutoIdentifier(text: string) {
  // unemoji 
  // TODO: if necessary
  // https://github.com/jgm/pandoc/blob/83880b0dbc318703babfbb6905b1046fa48f1216/src/Text/Pandoc/Shared.hs#L528
  for (const emjoi of emojis(SkinTone.None)) {
    emojiForAllSkinTones(emjoi).forEach(skinToneEmoji => {
      const charLoc = text.indexOf(skinToneEmoji.emoji);
      if (charLoc !== -1) {
        text = text.substr(0, charLoc) + skinToneEmoji.aliases[0] + text.substr(charLoc + skinToneEmoji.emoji.length);
      }
    });
  }

  // other transformations
  // https://github.com/jgm/pandoc/blob/83880b0dbc318703babfbb6905b1046fa48f1216/src/Text/Pandoc/Shared.hs#L539
  return text
    .replace(/[^ \t\w\-_]/g, '')  // filterPunct
    .replace(/[ \t]/g, '-')       // spaceToDash
    .toLowerCase();               // toLower
}
