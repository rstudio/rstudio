import { wrappingInputRule } from 'prosemirror-inputrules';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';

import { WrapCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';

const extension: Extension = {
  nodes: [
    {
      name: 'blockquote',
      spec: {
        content: 'block+',
        group: 'block',
        defining: true,
        parseDOM: [{ tag: 'blockquote' }],
        toDOM() {
          return ['blockquote', { class: 'pm-blockquote pm-block-border-color' }, 0];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.BlockQuote,
            block: 'blockquote',
          },
        ],
        writer: (output: PandocOutput, node: ProsemirrorNode) => {
          output.writeToken(PandocTokenType.BlockQuote, () => {
            output.writeNodes(node);
          });
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [new WrapCommand(EditorCommandId.Blockquote, ['Mod->'], schema.nodes.blockquote)];
  },

  inputRules: (schema: Schema) => {
    return [wrappingInputRule(/^\s*>\s$/, schema.nodes.blockquote)];
  },
};

export default extension;
