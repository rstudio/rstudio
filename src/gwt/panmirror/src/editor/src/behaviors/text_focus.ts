/*
 * text_focus.ts
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import { DecorationSet } from 'prosemirror-view';
import { Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';
import { findParentNode } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { nodeDecoration } from '../api/decoration';

const key = new PluginKey('text_focus');

const extension: Extension = {
  plugins: () => {
    return [
      new Plugin<DecorationSet>({
        key,
        state: {
          init(_config: { [key: string]: any }) {
            return DecorationSet.empty;
          },

          apply(tr: Transaction, set: DecorationSet, oldState: EditorState, newState: EditorState) {
            // check for selection chnage
            if (tr.selectionSet || !oldState.selection.eq(newState.selection)) {
              const term = findParentNode(node => {
                return node.isTextblock;
              })(newState.selection);
              if (term) {
                return DecorationSet.create(newState.doc, [
                  nodeDecoration(term, { class: 'pm-text-focused pm-focus-outline-color' }),
                ]);
              } else {
                return DecorationSet.empty;
              }
            } else {
              return set;
            }
          },
        },
        props: {
          decorations(state: EditorState) {
            return key.getState(state);
          },
        },
      }),
    ];
  },
};

export default extension;
