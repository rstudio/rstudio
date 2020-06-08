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

import { EditorUI } from '../../api/ui';

import { CompletionHandler, CompletionResult } from "../../api/completion";
import { emojis, Emoji, SkinTone, emojiFromChar, emojiForAllSkinTones } from "../../api/emoji";
import { getMarkRange } from '../../api/mark';
import { nodeForEmoji } from './emoji';

export function emojiCompletionHandler(ui: EditorUI) : CompletionHandler<Emoji> {

  return {
    
    completions: emojiCompletions(ui),

    replacement(schema: Schema, emoji: Emoji | null) : string | ProsemirrorNode | null {
      if (emoji) {
        return nodeForEmoji(schema, emoji, emoji.aliases[0]);
      } else {
        return null;
      }
    },

    view: {
      component: EmojiView,
      key: emoji => emoji.emoji,
      width: 200,
      hideNoResults: true
    },

  };
}

const kMaxEmojiCompletions = 20;
const kEmojiCompletionRegEx = /(^|[^`]):(\w{2,})$/;

function emojiCompletions(ui: EditorUI) {

  return (text: string, _doc: ProsemirrorNode, selection: Selection): CompletionResult<Emoji> | null => {
  
    // look for requisite text sequence
    const match = text.match(kEmojiCompletionRegEx);
    if (match) {
  
      // determine insert position and prefix to search for
      const prefix = match[2].toLowerCase();
      const pos = selection.head - prefix.length - 1; // -1 for the leading :
  
      // scan for completions that match the prefix (truncate as necessary)
      const completions: Emoji[] = [];
      for (const emoji of emojis(ui.prefs.emojiSkinTone())) {
        const alias = emoji.aliases.find(a => a.startsWith(prefix));
        if (alias) {
          completions.push({
            ...emoji,
            aliases: [alias],
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
  };
}

const EmojiView: React.FC<Emoji> = emoji => {
  return (
    <div className={'pm-completion-item-text'}>
      {emoji.emoji}&nbsp;:{emoji.aliases[0]}:
    </div>
  );
};


export function emojiSkintonePreferenceCompletionHandler(ui: EditorUI) : CompletionHandler<Emoji> {

  return {
    
    completions: emojiSkintonePreferenceCompletions(ui),

    replacement(schema: Schema, emoji: Emoji | null) : string | ProsemirrorNode | null {

      // JJA: a null emoji means the user pressed 'Esc', we probably 
      // want this to mean "use default" (so the prompt doesn't occur again)

      if (emoji) {

        // Save this preference and use it in the future
        ui.prefs.setEmojiSkinTone(emoji.skinTone);

        // Emit the emoji of the correct skin tone
        return nodeForEmoji(schema, emoji, emoji.aliases[0]);

      } else {
        
        return null;

      }
     
    },

    view: {

      // JJA: implement header w/ "prompt" (may want this to also indicate that 
      // this can be also be (re)set later in application preferences -- not 
      // sure how you do that w/o getting too wordy)
      header: {
        component: EmojiSkintonePreferenceHeaderView,
        height: 25
      },
      
      component: EmojiSkintonePreferenceView,
      key: pref => pref.skinTone,
      width: 50,
      horizontal: true
    },

  };
}

function emojiSkintonePreferenceCompletions(ui: EditorUI) {
  return (text: string, doc: ProsemirrorNode, selection: Selection): CompletionResult<Emoji> | null => {
    
    // The user has set a preference for skin tone
    if (ui.prefs.emojiSkinTone() !== SkinTone.None) {
      return null;
    }
    
    const range = getMarkRange(doc.resolve(selection.head-1), doc.type.schema.marks.emoji);
    if (!range) {
      return null;
    }
    
    const prefs = new Array<Emoji>();
    const emojiText = doc.textBetween(range.from, range.to);
    const emoji = emojiFromChar(emojiText);
    
    // If this is an emoji that support skin tones
    if (emoji && emoji.supportsSkinTone) {
      prefs.push(...emojiForAllSkinTones(emoji));
    }

    return {
      pos: range.from,
      completions: () => Promise.resolve(prefs)
    };
  };
}

const EmojiSkintonePreferenceHeaderView: React.FC = () => {
  return (
    <span>Header</span>
  );
};

const EmojiSkintonePreferenceView: React.FC<Emoji> = emoji => {
  return (
    <div className={'pm-completion-item-text'}>
      {emoji.emoji}
    </div>
  );
};





