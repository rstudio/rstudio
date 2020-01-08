import { Schema } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { Extension } from '../api/extension';
import { BaseKey } from '../api/basekeys';
import { PandocOutput, PandocTokenType } from '../api/pandoc';

const extension: Extension = {
  nodes: [
    {
      name: 'hard_break',
      spec: {
        inline: true,
        group: 'inline',
        selectable: false,
        parseDOM: [{ tag: 'br' }],
        toDOM() {
          return ['br'];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.LineBreak,
            node: 'hard_break',
          },
        ],
        writer: (output: PandocOutput) => {
          output.writeToken(PandocTokenType.LineBreak);
        },
      },
    },
  ],

  baseKeys: (_schema: Schema) => {
    return [
      { key: BaseKey.ModEnter, command: hardBreakCommandFn() },
      { key: BaseKey.ShiftEnter, command: hardBreakCommandFn() },
    ];
  },
};

function hardBreakCommandFn() {
  return (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
    const br = state.schema.nodes.hard_break;
    if (dispatch) {
      dispatch(state.tr.replaceSelectionWith(br.create()).scrollIntoView());
    }
    return true;
  };
}

export default extension;
