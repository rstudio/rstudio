/*
 * emoji.ts
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

// read emojies from json (https://github.com/jgm/emojis/blob/master/emoji.json)
import kEmojisJson from './emoji.json';
const kEmojis = kEmojisJson as Emoji[];

export interface Emoji {
  emoji: string;
  aliases: string[];
}

export function emojies() {
  return kEmojis;
}

export function emojiFromChar(emojiChar: string): Emoji | undefined {
  return kEmojis.find(emoji => emoji.emoji === emojiChar);
}

export function emojiFromAlias(emojiAlias: string): Emoji | undefined {
  for (const emoji of kEmojis) {
    if (emoji.aliases.includes(emojiAlias)) {
      return emoji;
    }
  }
  return undefined;
}
