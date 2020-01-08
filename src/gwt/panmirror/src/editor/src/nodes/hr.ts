import { Schema } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { findParentNodeOfType } from 'prosemirror-utils';
import { EditorState } from 'prosemirror-state';

import { ProsemirrorCommand, insertNode, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';

import './hr-styles.css';

const extension: Extension = {
  nodes: [
    {
      name: 'horizontal_rule',
      spec: {
        group: 'block',
        parseDOM: [{ tag: 'hr' }],
        toDOM() {
          return ['div', ['hr', { class: 'pm-border-background-color' }]];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.HorizontalRule,
            node: 'horizontal_rule',
          },
        ],
        writer: (output: PandocOutput) => {
          output.writeToken(PandocTokenType.HorizontalRule);
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [
      new ProsemirrorCommand(EditorCommandId.HorizontalRule, ['Mod-_'], insertNode(schema.nodes.horizontal_rule)),
    ];
  },

  inputRules: (_schema: Schema) => {
    return [
      new InputRule(/^\*{3}$/, (state: EditorState, match: string[], start: number, end: number) => {
        const schema = state.schema;
        const paraNode = findParentNodeOfType(schema.nodes.paragraph)(state.selection);
        if (paraNode && state.selection.$anchor.depth === 2) {
          // only in top-level paragraphs
          return state.tr.replaceRangeWith(start, end, schema.nodes.horizontal_rule.create());
        } else {
          return null;
        }
      }),
    ];
  },
};

export default extension;
