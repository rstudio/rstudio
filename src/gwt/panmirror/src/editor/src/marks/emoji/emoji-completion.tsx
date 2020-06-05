/*
 * emoji-completion.tsx
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

import { Selection } from 'prosemirror-state';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';

import React from 'react';

import { CompletionHandler, CompletionResult } from "../../api/completion";
import { Emoji, emojis } from "../../api/emoji";

export function emojiCompletionHandler() : CompletionHandler<Emoji> {

  return {
    
    completions: emojiCompletions,

    replacement(schema: Schema, emoji: Emoji) : string | ProsemirrorNode {
      const mark = schema.marks.emoji.create({ emojihint: emoji.aliases[0] });
      return schema.text(emoji.emoji, [mark]);
    },

    view: {
      component: EmojiView,
      key: emoji => emoji.emoji,
      width: 200
    },

  };
}

const kMaxEmojiCompletions = 20;
const kEmojiCompletionRegEx = /(^|[^`]):(\w{2,})$/;

function emojiCompletions(text: string, selection: Selection): CompletionResult<Emoji> | null {
  
  // look for requisite text sequence
  const match = text.match(kEmojiCompletionRegEx);
  if (match) {

    // determine insert position and prefix to search for
    const prefix = match[2].toLowerCase();
    const pos = selection.head - prefix.length - 1; // -1 for the leading :

    // scan for completions that match the prefix (truncate as necessary)
    const completions: Emoji[] = [];
    for (const emoji of emojis()) {
      const alias = emoji.aliases.find(a => a.startsWith(prefix));
      if (alias) {
        completions.push({
          emoji: emoji.emoji,
          aliases: [alias]
        });
      }
      if (completions.length >= kMaxEmojiCompletions) {
        break;
      }
    }

    // return result
    return { 
      pos, 
      completions: () => Promise.resolve(completions) 
    };

  // no match
  } else {
    return null;
  }
}


const EmojiView: React.FC<Emoji> = emoji => {
  return (
    <div className={'pm-completion-item-text'}>
      {emoji.emoji}&nbsp;:{emoji.aliases[0]}:
    </div>
  );
};
