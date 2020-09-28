/*
 * smarty.ts
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

import { ellipsis, InputRule } from 'prosemirror-inputrules';
import { Plugin, PluginKey, EditorState } from 'prosemirror-state';
import { Schema } from 'prosemirror-model';

import { Extension, extensionIfEnabled } from '../api/extension';
import { fancyQuotesToSimple } from '../api/quote';

const plugin = new PluginKey('smartypaste');

// match enDash but only for lines that aren't an html comment
const enDash = new InputRule(/[^!-`]--$/, (state: EditorState, match: string[], start: number, end: number) => {
  const { parent, parentOffset } = state.selection.$head;
  const precedingText = parent.textBetween(0, parentOffset);
  if (precedingText.indexOf('<!--') === -1) {
    const tr = state.tr;
    tr.insertText('–', end - 1, end);
    return tr;
  } else {
    return null;
  }
});

const emDash = new InputRule(/(^|[^`])–-$/, (state: EditorState, match: string[], start: number, end: number) => {
  const tr = state.tr;
  tr.insertText('—', end - 1, end);
  return tr;
});


const extension: Extension = {
  inputRules: () => {
    return [ellipsis, enDash, emDash];
  },

  plugins: (schema: Schema) => {
    return [
      // apply smarty rules to plain text pastes
      new Plugin({
        key: plugin,
        props: {
          transformPastedText(text: string) {

            // emdash
            text = text.replace(/(\w)---(\w)/g, '$1—$2');

            // endash
            text = text.replace(/(\w)--(\w)/g, '$1–$2');

            // ellipses
            text = text.replace(/\.\.\./g, '…');

            // we explicitly don't want fancy quotes in the editor
            text = fancyQuotesToSimple(text);

            return text;
          },
        },
      }),
    ];
  },
};

export default extensionIfEnabled(extension, 'smart');
