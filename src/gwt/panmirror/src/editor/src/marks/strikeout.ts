import { Schema, Mark, Fragment } from 'prosemirror-model';

import { MarkCommand, EditorCommandId } from '../api/command';
import { Extension, extensionIfEnabled } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';
import { delimiterMarkInputRule } from '../api/mark';

const extension: Extension = {
  marks: [
    {
      name: 'strikeout',
      spec: {
        parseDOM: [
          { tag: 'del' },
          { tag: 's' },
          {
            style: 'text-decoration',
            getAttrs: (value: string | Node) => (value as string) === 'line-through' && null,
          },
        ],
        toDOM() {
          return ['del'];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Strikeout,
            mark: 'strikeout',
          },
        ],
        writer: {
          priority: 5,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
            output.writeMark(PandocTokenType.Strikeout, parent);
          },
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [new MarkCommand(EditorCommandId.Strikeout, [], schema.marks.strikeout)];
  },

  inputRules: (schema: Schema) => {
    return [delimiterMarkInputRule('~~', schema.marks.strikeout)];
  },
};

export default extensionIfEnabled(extension, 'strikeout');
