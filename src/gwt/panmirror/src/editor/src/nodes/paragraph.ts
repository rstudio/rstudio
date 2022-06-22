/*
 * paragraph.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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
import { emptyNodePlaceholderPlugin } from '../api/placeholder';
import { selectionWithinLastBodyParagraph } from '../api/selection';
import { kAddToHistoryTransaction } from '../api/transaction';

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

  plugins: (schema: Schema) => {
    let showHint = true;
    return [emptyNodePlaceholderPlugin(schema.nodes.paragraph, node => " type / to insert a block (code, math, figure, div, etc.)", tr => {
      if (showHint !== false) {
        if (selectionWithinLastBodyParagraph(tr.selection)) {
          if (tr.docChanged) {
            showHint = false;
            return false;
          } else {
            return true;
          }
        } else {
          return false;
        }
      } else {
        return false;
      }
    })];
  },
};

class InsertParagraphCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.ParagraphInsert, [], insertParagraph);
  }
}

export default extension;
