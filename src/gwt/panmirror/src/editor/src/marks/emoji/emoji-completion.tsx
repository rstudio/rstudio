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

import { EditorState } from 'prosemirror-state';
import { Node as ProsemirrorNode } from 'prosemirror-model';

import React from 'react';

import { CompletionHandler } from "../../api/completion";
import { Emoji, emojisFromPrefx } from "../../api/emoji";

export function emojiCompletionHandler() : CompletionHandler {

  return {
    canCompleteAt(state: EditorState): number | null {
      const match = matchEmojiCompletion(state); 
      if (match) {
        return state.selection.head - match[2].length - 1;
      } else {
        return null;
      }
    },


    completions: (state: EditorState, limit: number): Promise<Emoji[]> => {

      let results = new Array<Emoji>();

      const match = matchEmojiCompletion(state);
      if (match) {

        // TODO: implement support for limit
        // TODO: ensure that multi-alias emojis prefer the matching one

        results = emojisFromPrefx(match[2]);
      }

      return Promise.resolve(results);
    },

    completionView: EmojiView,

    replacement(emoji: Emoji) : string | ProsemirrorNode {
      return emoji.emoji;
    }

  };
}

const kMaxEmojiLength = 50;
const kEmojiCompletionRegEx = /(^|[^`]):(\w{2,})$/;

function matchEmojiCompletion(state: EditorState) {

  // inspect the text of the parent up to 50 characters back
  const { $head } = state.selection;
  
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
