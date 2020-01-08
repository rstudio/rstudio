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
