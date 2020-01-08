import { Extension } from '../api/extension';
import { PandocOutput, PandocToken, PandocTokenType } from '../api/pandoc';

const extension: Extension = {
  nodes: [
    {
      name: 'soft_break',
      spec: {
        inline: true,
        content: 'text*',
        group: 'inline',
        parseDOM: [{ tag: "span[class*='soft-break']" }],
        toDOM() {
          return ['span', { class: 'soft-break' }, 0];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.SoftBreak,
            node: 'soft_break',
            getText: (tok: PandocToken) => ' ',
          },
        ],
        writer: (output: PandocOutput) => {
          output.writeToken(PandocTokenType.SoftBreak);
        },
      },
    },
  ],
};

export default extension;
