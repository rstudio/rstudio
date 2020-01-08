import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { setTextSelection } from 'prosemirror-utils';

import { canInsertNode } from '../api/node';
import { BlockCommand, EditorCommandId, ProsemirrorCommand } from '../api/command';
import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';

const extension: Extension = {
  nodes: [
    {
      name: 'paragraph',
      spec: {
        content: 'inline*',
        group: 'block',
        parseDOM: [{ tag: 'p' }],
        toDOM() {
          return ['p', 0];
        },
      },
      pandoc: {
        readers: [
          { token: PandocTokenType.Para, block: 'paragraph' },
          { token: PandocTokenType.Plain, block: 'paragraph' },
        ],
        writer: (output: PandocOutput, node: ProsemirrorNode) => {
          output.writeToken(PandocTokenType.Para, () => {
            output.writeInlines(node.content);
          });
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [
      new BlockCommand(EditorCommandId.Paragraph, ['Shift-Ctrl-0'], schema.nodes.paragraph, schema.nodes.paragraph),
      new InsertParagraphCommand(),
    ];
  },
};

class InsertParagraphCommand extends ProsemirrorCommand {
  constructor() {
    super(
      EditorCommandId.ParagraphInsert,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        if (!canInsertNode(state, schema.nodes.paragraph)) {
          return false;
        }

        if (dispatch) {
          const tr = state.tr;
          tr.replaceSelectionWith(schema.nodes.paragraph.create());
          setTextSelection(state.selection.from, -1)(tr);
          dispatch(tr);
        }

        return true;
      },
    );
  }
}

export default extension;
