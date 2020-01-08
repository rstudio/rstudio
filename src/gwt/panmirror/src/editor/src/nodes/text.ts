import { Node as ProsemirrorNode } from 'prosemirror-model';

import { Extension } from '../api/extension';
import { PandocOutput, PandocToken, PandocTokenType } from '../api/pandoc';

const extension: Extension = {
  nodes: [
    {
      name: 'text',
      spec: {
        group: 'inline',
        toDOM(node: ProsemirrorNode): any {
          return node.text;
        },
      },
      pandoc: {
        readers: [
          { token: PandocTokenType.Str, text: true, getText: (tok: PandocToken) => tok.c },
          { token: PandocTokenType.Space, text: true, getText: () => ' ' },
        ],
        writer: (output: PandocOutput, node: ProsemirrorNode) => {
          let text = node.textContent;

          // we allow escaping of \ and < (to avoid creating unintential raw inlines)
          text = text.replace(/\\\\(\w)/, '\\$1');
          text = text.replace('\\<', '<');

          output.writeText(text);
        },
      },
    },
  ],
};

export default extension;
