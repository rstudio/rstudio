/*
 * nbsp.ts
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



import { DecorationSet, Decoration } from "prosemirror-view";
import { Plugin, PluginKey, EditorState, Transaction } from "prosemirror-state";
import { Node as ProsemirrorNode } from "prosemirror-model";

import { EditorCommandId, InsertCharacterCommand } from "../api/command";
import { forChangedNodes } from "../api/transaction";
import { mergedTextNodes } from "../api/text";

const kNbsp = '\u00A0';

const extension = {

  commands: () => {
    return [
      new InsertCharacterCommand(EditorCommandId.NonBreakingSpace, kNbsp, ['Ctrl-Space', 'Ctrl-Shift-Space'])
    ];
  },

  plugins: () => {
    return [
      nonBreakingSpaceHighlightPlugin()
    ];
  }
};

const pluginKey = new PluginKey('nbsp-highlight');

function nonBreakingSpaceHighlightPlugin() {
  return new Plugin<DecorationSet>({
    key: pluginKey,
    state: {
      init(_config: { [key: string]: any }, instance: EditorState) {
        return DecorationSet.create(instance.doc, highlightNode(instance.doc));
      },
      apply(tr: Transaction, set: DecorationSet, oldState: EditorState, newState: EditorState) {

        // map
        set = set.map(tr.mapping, tr.doc);

        // find new
        if (tr.docChanged) {
          const decorations: Decoration[] = [];
          forChangedNodes(oldState, newState,
            node => node.isTextblock && node.textContent.includes(kNbsp),
            (node, pos) => {
              decorations.push(...highlightNode(node, pos + 1));
            });
          set = set.add(tr.doc, decorations);
        }

        // return the set
        return set;
      },
    },
    props: {
      decorations(state: EditorState) {
        return pluginKey.getState(state);
      },
    },
  });
}

const kHighlightRegEx = /\xA0+/g;

function highlightNode(node: ProsemirrorNode, nodePos = 0) {
  const decorations: Decoration[] = [];
  const textNodes = mergedTextNodes(node);
  textNodes.forEach(textNode => {
    const text = textNode.text;
    let m;
    kHighlightRegEx.lastIndex = 0;
    // tslint:disable-next-line no-conditional-assignment
    while ((m = kHighlightRegEx.exec(text))) {
      if (m[0] === '') {
        break;
      }
      const from = nodePos + textNode.pos + m.index;
      const to = nodePos + textNode.pos + m.index + m[0].length;
      const classes = ['pm-nbsp', 'pm-span-background-color'];
      decorations.push(Decoration.inline(from, to, { class: classes.join(' ') }));
    }
    kHighlightRegEx.lastIndex = 0;
  });
  return decorations;
}


export default extension;
