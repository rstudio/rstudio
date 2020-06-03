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
import { Node as ProsemirrorNode } from 'prosemirror-model';

import React from 'react';

import { CompletionHandler, CompletionResult } from "../../api/completion";
import { Emoji, emojisFromPrefx } from "../../api/emoji";

const kEmojiCompletionRegEx = /(^|[^`]):(\w{2,})$/;

export function emojiCompletionHandler() : CompletionHandler<Emoji> {

  return {
    
    completions: (text: string, selection: Selection): CompletionResult<Emoji> | null  => {
      const match = text.match(kEmojiCompletionRegEx);
      if (match) {
        return {
          pos: selection.head - match[2].length - 1,
          items: emojisFromPrefx(match[2])
        };
      } else {
        return null;
      }
    },

    replacement(emoji: Emoji) : string | ProsemirrorNode {
      return emoji.emoji;
    },

    view: {
      component: EmojiView,
      width: 200
    },

  };
}


const EmojiView: React.FC<Emoji> = emoji => {
  return (
    <div>{emoji.emoji}&nbsp;:{emoji.aliases[0]}:</div>
  );
};
