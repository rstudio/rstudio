import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';

import { Extension, extensionIfEnabled } from '../api/extension';
import { PandocOutput, PandocTokenType, PandocToken } from '../api/pandoc';

import { EditorCommandId, WrapCommand } from '../api/command';

import './line_block-styles.css';

const extension: Extension = {
  nodes: [
    {
      name: 'line_block',
      spec: {
        content: 'paragraph+',
        group: 'block',
        parseDOM: [
          {
            tag: "div[class*='line-block']",
          },
        ],
        toDOM() {
          return ['div', { class: 'line-block pm-line-block pm-block-border-color pm-margin-bordered' }, 0];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.LineBlock,
            block: 'line_block',
            getChildren: (tok: PandocToken) => {
              return tok.c.map((line: PandocToken[]) => ({ t: PandocTokenType.Para, c: line }));
            },
          },
        ],
        writer: (output: PandocOutput, node: ProsemirrorNode) => {
          output.withOption('writeSpaces', false, () => {
            output.writeToken(PandocTokenType.LineBlock, () => {
              node.forEach(line => {
                output.writeArray(() => {
                  output.writeInlines(line.content);
                });
              });
            });
          });
        },
      },
    },
  ],
  commands: (schema: Schema) => {
    return [new WrapCommand(EditorCommandId.LineBlock, [], schema.nodes.line_block)];
  },
};

export default extensionIfEnabled(extension, 'line_blocks');
