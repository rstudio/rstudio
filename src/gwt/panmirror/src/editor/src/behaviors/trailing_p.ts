import { Plugin, PluginKey, EditorState, Transaction, Selection } from 'prosemirror-state';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';

import { Extension } from '../api/extension';
import { editingRootNode } from '../api/node';

const plugin = new PluginKey('trailingp');

const extension: Extension = {
  plugins: (schema: Schema) => {
    return [
      new Plugin({
        key: plugin,
        view: () => ({
          update: view => {
            const { state } = view;
            const insertNodeAtEnd = plugin.getState(state);
            if (!insertNodeAtEnd) {
              return;
            }

            // insert paragraph at the end of the editing root
            const tr = state.tr;
            const type = schema.nodes.paragraph;
            const editingNode = editingRootNode(tr.selection);
            if (editingNode) {
              tr.insert(editingNode.pos + editingNode.node.nodeSize - 1, type.create());
              view.dispatch(tr);
            }
          },
        }),
        state: {
          init: (_config, state: EditorState) => {
            return false;
          },
          apply: (tr: Transaction, value: any) => {
            if (!tr.docChanged) {
              return value;
            }
            return insertTrailingP(tr.selection);
          },
        },
      }),
    ];
  },
};

function insertTrailingP(selection: Selection) {
  const editingRoot = editingRootNode(selection);
  if (editingRoot) {
    return !isParagraphNode(editingRoot.node.lastChild);
  } else {
    return false;
  }
}

function isParagraphNode(node: ProsemirrorNode | null | undefined) {
  if (node) {
    const schema = node.type.schema;
    return node.type === schema.nodes.paragraph;
  } else {
    return false;
  }
}

export default extension;
