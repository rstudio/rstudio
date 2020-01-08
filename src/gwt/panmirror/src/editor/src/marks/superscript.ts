import { Schema, Mark, Fragment } from 'prosemirror-model';

import { MarkCommand, EditorCommandId } from '../api/command';
import { Extension, extensionIfEnabled } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';
import { delimiterMarkInputRule } from '../api/mark';

const extension: Extension = {
  marks: [
    {
      name: 'superscript',
      spec: {
        parseDOM: [{ tag: 'sup' }],
        toDOM() {
          return ['sup'];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Superscript,
            mark: 'superscript',
          },
        ],
        writer: {
          priority: 10,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
            output.writeMark(PandocTokenType.Superscript, parent);
          },
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [new MarkCommand(EditorCommandId.Superscript, [], schema.marks.superscript)];
  },

  inputRules: (schema: Schema) => {
    return [delimiterMarkInputRule('\\^', schema.marks.superscript)];
  },
};

export default extensionIfEnabled(extension, 'superscript');
