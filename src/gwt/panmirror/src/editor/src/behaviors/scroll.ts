/*
 * scroll.ts
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

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorState, Transaction, TextSelection, Selection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { findBlockNodes, ContentNodeWithPos } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { editingRootNode } from '../api/node';

const SCROLL_PADDING_PX = 40;

const extension: Extension = {
  commands: () => {
    return [
      new ProsemirrorCommand(EditorCommandId.ScrollPageUp, ['PageUp'], scrollPageUp),
      new ProsemirrorCommand(EditorCommandId.ScrollPageDown, ['PageDown'], scrollPageDown)
    ];
  },
};

export function scrollPageUp(state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) {
  return scrollPage(-1, state, dispatch, view);
}

export function scrollPageDown(state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) {
  return scrollPage(1, state, dispatch, view);
}

function scrollPage(pages: number, state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) {
  if (dispatch) {
    dispatch(state.tr);
    if (view) {
      var bodyEl = view.dom.firstElementChild;
      if (bodyEl) {
        var rect = bodyEl.getBoundingClientRect();
        var scrollBy = Math.max(0, rect.height - SCROLL_PADDING_PX);
        bodyEl.scrollTop += (scrollBy * pages);
      }
    }
  }
  return true;

}

export default extension;

