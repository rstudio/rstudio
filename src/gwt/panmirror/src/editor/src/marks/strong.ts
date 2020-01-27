/*
 * strong.ts
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

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
