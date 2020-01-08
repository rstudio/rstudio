import { Schema, Mark, Fragment } from 'prosemirror-model';

import { MarkCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';
import { delimiterMarkInputRule } from '../api/mark';

const extension: Extension = {
  marks: [
    {
      name: 'em',
      spec: {
        parseDOM: [
          { tag: 'i' },
          { tag: 'em' },
          { style: 'font-weight', getAttrs: (value: string | Node) => (value as string) === 'italic' && null },
        ],
        toDOM() {
          return ['em'];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.Emph,
            mark: 'em',
          },
        ],
        writer: {
          priority: 2,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
            output.writeMark(PandocTokenType.Emph, parent, true);
          },
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [new MarkCommand(EditorCommandId.Em, ['Mod-i'], schema.marks.em)];
  },

  inputRules: (schema: Schema) => {
    return [delimiterMarkInputRule('\\*', schema.marks.em, '\\*')];
  },
};

export default extension;
