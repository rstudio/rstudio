/*
 * insert_emoji.tsx
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

 // JJA
// TODO: Multicharacter emoji aren't being handled properly- CLEAN UP IMPLEMENTATION
// TODO: Discuss taking larger emoji file and what to do about that
    // 330k, thinned to 253k, zips to 27k

// Core - phase 1
// TODO: Group Emoji
// Common at the front? in a seprate section?


// TODO: Filter/search all aliases?
// TODO: emoji don't look centered in grid cell? Need fixed font?
// TODO: Insert text node with emoji mark, not raw characters

// TODO: Find any strings and make sure they use translate
// TODO: For symbol category names, what is the right way to localize the names (at what level should this happen)?

// Skin tone, ensure gender represented appropriately
// global option for skin tone, and a per document option (markdown writer option)
// TODO: Skin tone - insert skin tone character after selected emoji for emoji that support it

import { Schema } from 'prosemirror-model';
import { PluginKey } from 'prosemirror-state';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { EditorEvents } from '../../api/events';
import { Extension } from '../../api/extension';
import { EditorFormat } from '../../api/format';
import { EditorOptions } from '../../api/options';
import { PandocExtensions } from '../../api/pandoc';
import { PandocCapabilities } from '../../api/pandoc_capabilities';
import { EditorUI } from '../../api/ui';

import { performInsertSymbol, InsertSymbolPlugin } from './insert_symbol-plugin';

import kEmojisJson from '../../api/emoji.json';
import { Emoji } from '../../api/emoji';
import { SymbolDataProvider, SymbolCharacter } from './insert_symbol-dataprovider';

const key = new PluginKey<boolean>('insert-emoji');

const extension = (
  _pandocExtensions: PandocExtensions,
  _pandocCapabilities: PandocCapabilities,
  ui: EditorUI,
  _format: EditorFormat,
  _options: EditorOptions,
  events: EditorEvents,
): Extension => {
  return {
    commands: () => {
      return [new ProsemirrorCommand(EditorCommandId.Emoji, [], performInsertSymbol(key))];
    },
    plugins: (_schema: Schema) => {
      return [new InsertSymbolPlugin(key, new EmojiSymbolDataProvider(), ui, events)];
    },
  };
};

export const kCategoryAll = 'All';
const kEmojis = kEmojisJson as Emoji[];

class EmojiSymbolDataProvider implements SymbolDataProvider {
  
  // TODO: use ui.translate call to translate
  public readonly filterPlaceholderHint = 'emoji name';

  public symbolGroupNames(): string[] {
    const categories: string[] = [];
    kEmojis.forEach(emoji => {
      if (!categories.includes(emoji.category)) {
        categories.push(emoji.category);
    }
    });
    return [kCategoryAll, ...categories];
  }

  public getSymbols(groupName: string | undefined) {
    if (groupName === kCategoryAll || groupName === undefined) {
      return kEmojis.map(emoji =>({ name: emoji.aliases[0], value: emoji.emoji}));
    }  else {
      return kEmojis.filter(emoji => emoji.category === groupName)
      .map(emoji =>({ name: emoji.aliases[0], value: emoji.emoji}));
    }
  }

  public filterSymbols(filterText: string, symbols: SymbolCharacter[]): SymbolCharacter[] {
    const filteredSymbols = symbols.filter(symbol => {
      // Search by alias
      if (symbol.name.includes(filterText)) {
        return true;
      }
      return false;
    });
    return filteredSymbols;
  }
}

export default extension;
