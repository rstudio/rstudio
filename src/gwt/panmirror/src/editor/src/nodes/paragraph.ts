/*
 * paragraph.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';

import { BlockCommand, EditorCommandId, ProsemirrorCommand } from '../api/command';
import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';
import { insertParagraph } from '../api/paragraph';
import { EditorUI } from '../api/ui';
import { OmniInsertGroup } from '../api/omni_insert';

const extension: Extension = {
  nodes: [
    {
      name: 'paragraph',
      spec: {
        content: 'inline*',
        group: 'block list_item_block',
        parseDOM: [{ tag: 'p' }],
        toDOM() {
          return ['p', 0];
        },
      },
      pandoc: {
        readers: [
          { token: PandocTokenType.Para, block: 'paragraph' },
          { token: PandocTokenType.Plain, block: 'paragraph' },
        ],
        writer: (output: PandocOutput, node: ProsemirrorNode) => {
          output.writeToken(PandocTokenType.Para, () => {
            output.writeInlines(node.content);
          });
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [
      new BlockCommand(EditorCommandId.Paragraph, ['Mod-Alt-0'], schema.nodes.paragraph, schema.nodes.paragraph),
      new InsertParagraphCommand(),
    ];
  },
};

class InsertParagraphCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.ParagraphInsert, [], insertParagraph);
  }
}

export default extension;
