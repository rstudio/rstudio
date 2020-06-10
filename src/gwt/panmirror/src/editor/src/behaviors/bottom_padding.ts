/*
 * bottom_padding.ts
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

import { Selection, Plugin, PluginKey, EditorState } from 'prosemirror-state';
import { Schema } from 'prosemirror-model';

import zenscroll from 'zenscroll';

import { Extension } from '../api/extension';
import { editingRootNode } from '../api/node';
import { selectionIsBodyTopLevel } from '../api/selection';
import { EditorView } from 'prosemirror-view';
import { bodyElement } from '../api/dom';
import { findParentNodeOfType } from 'prosemirror-utils';

// when we get close to the bottom, we autoscroll to provide more padding
const kAutoscrollGapPx = 25;

const extension: Extension = {
  plugins: (schema: Schema) => {
    return [
      new Plugin({
        key: new PluginKey('bottom_padding'),
        view: () => ({
          update: (view: EditorView, prevState: EditorState) => {
            const selection = view.state.selection;
            if (selectionWithinLastBodyParagraph(selection)) {
              const paragraphNode = findParentNodeOfType(schema.nodes.paragraph)(selection);
              if (paragraphNode) {
                const paragraphEl = view.nodeDOM(paragraphNode.pos) as HTMLElement;
                const paragraphRect = paragraphEl.getBoundingClientRect();
                const editorEl = view.dom;
                const editorRect = editorEl.getBoundingClientRect();
                if (Math.abs(paragraphRect.bottom - editorRect.bottom) < kAutoscrollGapPx) {
                  const bodyEl = bodyElement(view);
                  const scroller = zenscroll.createScroller(bodyEl);
                  scroller.toY(bodyEl.scrollHeight, 0);
                }
              }
            }
          },
        }),
      }),
    ];
  },
};

function selectionWithinLastBodyParagraph(selection: Selection) {
  if (selectionIsBodyTopLevel(selection)) {
    const editingRoot = editingRootNode(selection);
    if (editingRoot) {
      const node = selection.$head.node();
      return node === editingRoot.node.lastChild && node.type === node.type.schema.nodes.paragraph;
    }
  }
  return false;
}

export default extension;
