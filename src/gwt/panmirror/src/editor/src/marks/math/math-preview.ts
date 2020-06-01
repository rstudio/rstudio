/*
 * math-preview.ts
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

import { Plugin, PluginKey, Transaction, EditorState } from "prosemirror-state";
import { DecorationSet, EditorView } from "prosemirror-view";

import { EditorUIMath } from "../../api/ui";

export function mathPreviewPlugin(uiMath: EditorUIMath) {
  return new MathPreviewPlugin(uiMath);
}

const key = new PluginKey<DecorationSet>('math-preview');

class MathPreviewPlugin extends Plugin<DecorationSet> {
  constructor(uiMath: EditorUIMath) {
    let editorView: EditorView;
    super({
      key,
      view(view: EditorView) {
        editorView = view;
        return {};
      },
      state: {
        init: (_config: { [key: string]: any }) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          

          
          return DecorationSet.empty;
         
        },
      },
      props: {
        decorations: (state: EditorState) => {
          return key.getState(state);
        },
      },
    });
  }
}