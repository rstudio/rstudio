import { Schema, Mark, Fragment } from 'prosemirror-model';

import { MarkCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';
import { delimiterMarkInputRule } from '../api/mark';

const extension: Extension = {
  marks: [
    {
      name: 'strong',
      spec: {
        parseDOM: [
          { tag: 'b' },
          { tag: 'strong' },
          {
            style: 'font-weight',
            getAttrs: (value: string | Node) => /^(bold(er)?|[5-9]\d{2,})$/.test(value as string) && null,
          },
        ],
        toDOM() {
          return ['strong'];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Strong,
            mark: 'strong',
          },
        ],
        writer: {
          priority: 1,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
            output.writeMark(PandocTokenType.Strong, parent, true);
          },
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [new MarkCommand(EditorCommandId.Strong, ['Mod-b'], schema.marks.strong)];
  },

  inputRules: (schema: Schema) => {
    return [delimiterMarkInputRule('\\*\\*', schema.marks.strong)];
  },
};

export default extension;
