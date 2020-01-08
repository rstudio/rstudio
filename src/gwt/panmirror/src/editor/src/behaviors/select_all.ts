import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorState, Transaction, TextSelection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { findBlockNodes, ContentNodeWithPos } from 'prosemirror-utils';

import { Extension } from '../api/extension';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { editingRootNode } from '../api/node';

const extension: Extension = {
  commands: () => {
    return [new ProsemirrorCommand(EditorCommandId.SelectAll, ['Mod-a'], selectAll)];
  },
};

export function selectAll(state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) {
  if (dispatch) {
    const editingRoot = editingRootNode(state.selection);
    if (editingRoot) {
      const tr = state.tr;
      tr.setSelection(childBlocksSelection(tr.doc, editingRoot));
      dispatch(tr);
      if (view) {
        // we do this to escape from embedded editors e.g. codemirror
        view.focus();
      }
    }
  }
  return true;
}

function childBlocksSelection(doc: ProsemirrorNode, parent: ContentNodeWithPos) {
  const blocks = findBlockNodes(parent.node);
  const begin = parent.start + blocks[0].pos + 1;
  const lastBlock = blocks[blocks.length - 1];
  const end = parent.start + lastBlock.pos + lastBlock.node.nodeSize;
  return TextSelection.create(doc, begin, end);
}

export default extension;
