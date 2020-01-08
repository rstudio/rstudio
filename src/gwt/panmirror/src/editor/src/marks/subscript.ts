import { Schema, Mark, Fragment } from 'prosemirror-model';

import { MarkCommand, EditorCommandId } from '../api/command';
import { Extension, extensionIfEnabled } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';
import { delimiterMarkInputRule } from '../api/mark';

const extension: Extension = {
  marks: [
    {
      name: 'subscript',
      spec: {
        parseDOM: [{ tag: 'sub' }],
        toDOM() {
          return ['sub'];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Subscript,
            mark: 'subscript',
          },
        ],
        writer: {
          priority: 9,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
            output.writeMark(PandocTokenType.Subscript, parent);
          },
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [new MarkCommand(EditorCommandId.Subscript, [], schema.marks.subscript)];
  },

  inputRules: (schema: Schema) => {
    return [delimiterMarkInputRule('\\~', schema.marks.subscript, '\\~')];
  },
};

export default extensionIfEnabled(extension, 'subscript');
