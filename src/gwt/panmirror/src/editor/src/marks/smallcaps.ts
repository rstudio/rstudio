import { Schema, Mark, Fragment } from 'prosemirror-model';

import { MarkCommand, EditorCommandId } from '../api/command';
import { Extension, extensionIfEnabled } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';

import './smallcaps-styles.css';

const extension: Extension = {
  marks: [
    {
      name: 'smallcaps',
      spec: {
        parseDOM: [
          { tag: "span[class*='smallcaps']" },
          { style: 'font-variant', getAttrs: (value: string | Node) => (value as string) === 'small-caps' && null },
        ],
        toDOM() {
          return ['span', { class: 'smallcaps' }];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.SmallCaps,
            mark: 'smallcaps',
          },
        ],
        writer: {
          priority: 7,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
            output.writeMark(PandocTokenType.SmallCaps, parent);
          },
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [new MarkCommand(EditorCommandId.Smallcaps, [], schema.marks.smallcaps)];
  },
};

export default extensionIfEnabled(extension, ['bracketed_spans', 'native_spans']);
