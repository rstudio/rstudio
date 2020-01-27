/*
 * subscript.ts
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
