/*
 * smarty.ts
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

import { smartQuotes, ellipsis, InputRule } from 'prosemirror-inputrules';
import { Plugin, PluginKey, EditorState } from 'prosemirror-state';
import { Schema } from 'prosemirror-model';

import { Extension, extensionIfEnabled } from '../api/extension';

const plugin = new PluginKey('smartypaste');

// match emDash but only for lines that aren't an html comment
const enDash = new InputRule(/^(?:[^-]*)--$/, (state: EditorState, match: string[], start: number, end: number) => {
  if (!state.doc.textBetween(start, end).startsWith('<!--')) {
    const tr = state.tr;
    tr.insertText('–', end - 2, end);
    return tr;
  } else {
    return null;
  }
});

const emDash = new InputRule(/^(?:.*)–-$/, (state: EditorState, match: string[], start: number, end: number) => {
  const tr = state.tr;
  tr.insertText('—', end - 2, end);
  return tr;
});

const extension: Extension = {
  inputRules: () => {
    return [...smartQuotes, ellipsis, enDash, emDash];
  },

  plugins: (schema: Schema) => {
    return [
      // apply smarty rules to plain text pastes
      new Plugin({
        key: plugin,
        props: {
          transformPastedText(text: string) {
            // double quotes
            text = text.replace(/(?:^|[\s{[(<'"\u2018\u201C])(")/g, x => {
              return x.slice(0, x.length - 1) + '“';
            });
            text = text.replace(/"/g, '”');

            // single quotes
            text = text.replace(/(?:^|[\s{[(<'"\u2018\u201C])(')/g, x => {
              return x.slice(0, x.length - 1) + '‘';
            });
            text = text.replace(/'/g, '’');

            // emdash
            text = text.replace(/(\w)--(\w)/, '$1—$2');

            // ellipses
            text = text.replace(/\.\.\./, '…');

            return text;
          },
        },
      }),
    ];
  },
};

export default extensionIfEnabled(extension, 'smart');
