import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';

import './null-styles.css';

const extension: Extension = {
  nodes: [
    {
      name: 'null',
      spec: {
        group: 'block',
        atom: true,
        selectable: false,
        parseDOM: [{ tag: "div[class*='null-block']" }],
        toDOM() {
          return ['div', { class: 'null-block' }];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Null,
            node: 'null',
          },
        ],
        writer: (output: PandocOutput) => {
          output.writeToken(PandocTokenType.Null);
        },
      },
    },
  ],
};

export default extension;
