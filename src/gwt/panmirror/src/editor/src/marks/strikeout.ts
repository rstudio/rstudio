/*
 * strikeout.ts
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
