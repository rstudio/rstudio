/*
 * placeholder.ts
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

import { Node as ProsemirrorNode, NodeType } from 'prosemirror-model';
import { EditorState, Transaction, Plugin, PluginKey } from 'prosemirror-state';
import { DecorationSet, Decoration, EditorView } from 'prosemirror-view';

import { findParentNode } from 'prosemirror-utils';

import { EditorUI } from './ui';

export function emptyNodePlaceholderPlugin(nodeType: NodeType, placeholder: (node: ProsemirrorNode) => string) {
  const pluginKey = new PluginKey(nodeType.name + '-empty-placeholder');

  return new Plugin<DecorationSet>({
    key: pluginKey,
    state: {
      init(_config: { [key: string]: any }, instance: EditorState) {
        return DecorationSet.empty;
      },
      apply(tr: Transaction, set: DecorationSet, oldState: EditorState, newState: EditorState) {
        // check for empty parent of our type
        const emptyNode = findParentNode(node => node.type === nodeType && node.childCount === 0)(tr.selection);
        if (emptyNode) {
          const decoration = placeholderDecoration(emptyNode.pos + 1, placeholder(emptyNode.node));
          return DecorationSet.create(tr.doc, [decoration]);
        } else {
          return DecorationSet.empty;
        }
      },
    },
    props: {
      decorations(state: EditorState) {
        return pluginKey.getState(state);
      },
    },
  });
}

export function placeholderDecoration(pos: number, text: string) {
  return Decoration.widget(pos, (_view: EditorView, _getPos: () => number) => {
    const placeholder = window.document.createElement('span');
    placeholder.classList.add('pm-placeholder-text-color');
    placeholder.innerText = text;
    return placeholder;
  });
}

export function iconAndTextPlaceholderDecoration(pos: number, icon: string, text: string) {
  return Decoration.widget(pos, (_view: EditorView, _getPos: () => number) => {
    const container = window.document.createElement('span');

    const iconImg = window.document.createElement('img');
    iconImg.classList.add('pm-placeholder-icon');
    iconImg.setAttribute('src', icon);

    const message = window.document.createElement('span');
    message.classList.add('pm-placeholder-text-color');
    message.classList.add('pm-placeholder-text');
    message.innerText = text;

    container.appendChild(iconImg);
    container.appendChild(message);
    return container;
  });
}

export function searchPlaceholderDecoration(pos: number, ui: EditorUI, text?: string) {
  return iconAndTextPlaceholderDecoration(pos, ui.images.search!, text || '');
}
