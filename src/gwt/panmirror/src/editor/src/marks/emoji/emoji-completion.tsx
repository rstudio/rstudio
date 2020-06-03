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

import { EditorState, Selection } from 'prosemirror-state';
import { Node as ProsemirrorNode } from 'prosemirror-model';

import React from 'react';

import { CompletionHandler, CompletionResult } from "../../api/completion";
import { Emoji, emojisFromPrefx } from "../../api/emoji";

export function emojiCompletionHandler() : CompletionHandler<Emoji> {

  return {
    
    completions: (selection: Selection, limit: number): CompletionResult<Emoji> | null  => {

      const match = matchEmojiCompletion(selection); 
      if (match) {
        return {
          pos: selection.head - match[2].length - 1,
          items: emojisFromPrefx(match[2])
        };
      } else {
        return null;
      }
    },

    completionView: EmojiView,

    replacement(emoji: Emoji) : string | ProsemirrorNode {
      return emoji.emoji;
    }

  };
}





const kMaxEmojiLength = 50;
const kEmojiCompletionRegEx = /(^|[^`]):(\w{2,})$/;

function matchEmojiCompletion(selection: Selection) {

  // inspect the text of the parent up to 50 characters back
  const { $head } = selection;
  
  const textBefore = $head.parent.textBetween(
    Math.max(0, $head.parentOffset - kMaxEmojiLength),  // start
    $head.parentOffset,                                 // end
    undefined,                                          // block separator
    "\ufffc"                                            // leaf char
  );   
  
  // run the regex
  return textBefore.match(kEmojiCompletionRegEx);
}

const EmojiView: React.FC<Emoji> = emoji => {
  return (
    <div>{emoji.emoji}</div>
  );
};
