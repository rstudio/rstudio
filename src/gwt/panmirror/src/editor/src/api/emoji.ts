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

// read emojis from json (https://github.com/jgm/emojis/blob/master/emoji.json)
import kEmojisJson from './emojis-all.json';
const kEmojis = kEmojisJson as Emoji[];

// TODO: Some skin tone emoji don't render properly - create skincode exclusion list in generate-symbols
// TODO: Rename
// EmojiRaw
export interface Emoji {
  emoji: string; // emojiRaw
  aliases: string[];
  category: string;
  description: string;
  skin_tones: boolean;
  markdown: boolean;
}

// Emoji
export interface EmojiWithSkinTone extends Emoji {
  emojiWithSkinTone: string;  // emoji
  skinTone: SkinTone;
}

export enum SkinTone {
  None = -1,
  Default = 0,
  Light = 0x1F3FB,
  MediumLight = 0x1F3FC,
  Medium = 0x1F3FD,
  MediumDark = 0x1F3FE,
  Dark = 0x1F3FF
}

function hasSkinTone(skinTone: SkinTone): boolean {
  return skinTone !== SkinTone.None && skinTone !== SkinTone.Default;
}

export function emojis(skinTone: SkinTone) : EmojiWithSkinTone[] {
  return kEmojis.map(emoji => emojiWithSkinTone(emoji, skinTone));
}

export function emojiCategories() : string[] {
  const categories: string[] = [];
  kEmojis.forEach(emoji => {
    if (!categories.includes(emoji.category)) {
      categories.push(emoji.category);
  }
  });
  return categories;
}

export function emojiFromString(emojiString: string, skinTone: SkinTone): EmojiWithSkinTone | undefined  {
  return emojis(skinTone).find(em => em.emojiWithSkinTone === emojiString);   
}

export function emojiWithSkinTonePreference(emoji: Emoji, skinTone: SkinTone) : EmojiWithSkinTone {
  return emojiWithSkinTone(emoji, skinTone);
}

// Find a matching non skin toned emoji for a given string
export function emojiFromChar(emojiString: string): Emoji | undefined {
  return kEmojis.find(emoji => emoji.emoji === emojiString);
}

// Returns a non skin tonned emoji for a given alias.
export function emojiFromAlias(emojiAlias: string): Emoji | undefined {
  for (const emoji of kEmojis) {
    if (emoji.aliases.includes(emojiAlias)) {
      return emoji;
    }
  }
  return undefined;
}

// Returns an array of skin toned emoji including the unskintoned emoji. If the emoji
// doesn't support skin tones, this returns the original emoji.
export function emojiForAllSkinTones(emoji: Emoji) : EmojiWithSkinTone[] {
  if (emoji.skin_tones) {
    return [
      emojiWithSkinTone(emoji, SkinTone.Default),
      emojiWithSkinTone(emoji, SkinTone.Light),
      emojiWithSkinTone(emoji, SkinTone.MediumLight),
      emojiWithSkinTone(emoji, SkinTone.Medium),
      emojiWithSkinTone(emoji, SkinTone.MediumDark),
      emojiWithSkinTone(emoji, SkinTone.Dark),
    ];
  } else {
    return [emojiWithSkinTone(emoji, SkinTone.Default)];
  }
}


// Returns a skin toned version of the emoji, or the original emoji if it
// doesn't support skin tones
function emojiWithSkinTone(emoji: Emoji, skinTone: SkinTone ) : EmojiWithSkinTone {
  if (!emoji.skin_tones) {
    return {...emoji, emojiWithSkinTone: emoji.emoji, skinTone: SkinTone.Default};
  }

  const skinToneEmoji : EmojiWithSkinTone = {
    emoji: emoji.emoji,
    aliases: emoji.aliases,
    category: emoji.category,
    description: emoji.description,
    skin_tones: emoji.skin_tones,
    markdown: emoji.markdown,
    emojiWithSkinTone: emoji.emoji + characterForSkinTone(skinTone), // 
    skinTone,
  };
  return skinToneEmoji;
}

// No skin tone returns an empty string, otherwise the skintone codepoint
// is converted into a string
function characterForSkinTone(skinTone: SkinTone) : string {
  return (hasSkinTone(skinTone) ? String.fromCodePoint(skinTone) : '');
}