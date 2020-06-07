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
const kEmojis = kEmojisJson as EmojiRaw[];

// PREFERENCE
// TODO: Skin tone preference appears to be lost when reloading after a code change - do I need to flush or commit or something?
// TODO: Add user preference in RStudio somewhere

// APPEARANCE
// TODO: Appearance of preference completion handler
// Preferred appearance
// [] | [] [] [] [] []

// +1
// TODO: Reuse the preference completion handler and trigger it when user clicks on emoji?
// TODO: If user clicks on emoji, cycle through the available skin tones?
// 

// A raw emoji which doesn't include skin tone information
export interface EmojiRaw {
  emojiRaw: string; 
  aliases: string[];
  category: string;
  description: string;
  supportsSkinTone: boolean;
  hasMarkdownRepresentation: boolean;
}

// A complete emoji that may additional render skintone
export interface Emoji extends EmojiRaw {
  emoji: string;  
  skinTone: SkinTone;
}

// Skin tones that are permitted for emoji
// None = user hasn't expressed a preference
// Default = don't apply a skin tone (use yellow emoji)
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

export function emojis(skinTone: SkinTone) : Emoji[] {
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

export function emojiFromString(emojiString: string, skinTone: SkinTone): Emoji | undefined  {
  return emojis(skinTone).find(em => em.emoji === emojiString);   
}

export function emojiWithSkinTonePreference(emoji: EmojiRaw, skinTone: SkinTone) : Emoji {
  return emojiWithSkinTone(emoji, skinTone);
}

// Find a matching non skin toned emoji for a given string
export function emojiFromChar(emojiString: string): EmojiRaw | undefined {
  return kEmojis.find(emoji => emoji.emojiRaw === emojiString);
}

// Returns a non skin tonned emoji for a given alias.
export function emojiFromAlias(emojiAlias: string): EmojiRaw | undefined {
  for (const emoji of kEmojis) {
    if (emoji.aliases.includes(emojiAlias)) {
      return emoji;
    }
  }
  return undefined;
}

// Returns an array of skin toned emoji including the unskintoned emoji. If the emoji
// doesn't support skin tones, this returns the original emoji.
export function emojiForAllSkinTones(emoji: EmojiRaw) : Emoji[] {
  if (emoji.supportsSkinTone) {
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
function emojiWithSkinTone(emoji: EmojiRaw, skinTone: SkinTone ) : Emoji {
  if (!emoji.supportsSkinTone) {
    return {...emoji, emoji: emoji.emojiRaw, skinTone: SkinTone.Default};
  }

  const skinToneEmoji : Emoji = {
    emojiRaw: emoji.emojiRaw,
    aliases: emoji.aliases,
    category: emoji.category,
    description: emoji.description,
    supportsSkinTone: emoji.supportsSkinTone,
    hasMarkdownRepresentation: emoji.hasMarkdownRepresentation,
    emoji: emoji.emojiRaw + characterForSkinTone(skinTone), // 
    skinTone,
  };
  return skinToneEmoji;
}

// No skin tone returns an empty string, otherwise the skintone codepoint
// is converted into a string
function characterForSkinTone(skinTone: SkinTone) : string {
  return (hasSkinTone(skinTone) ? String.fromCodePoint(skinTone) : '');
}