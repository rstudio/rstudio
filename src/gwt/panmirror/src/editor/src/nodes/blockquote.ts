/*
 * blockquote.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import { wrappingInputRule } from 'prosemirror-inputrules';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';

import { WrapCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';

const extension: Extension = {
  nodes: [
    {
      name: 'blockquote',
      spec: {
        content: 'block+',
        group: 'block',
        defining: true,
        parseDOM: [{ tag: 'blockquote' }],
        toDOM() {
          return ['blockquote', { class: 'pm-blockquote pm-block-border-color' }, 0];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.BlockQuote,
            block: 'blockquote',
          },
        ],
        writer: (output: PandocOutput, node: ProsemirrorNode) => {
          output.writeToken(PandocTokenType.BlockQuote, () => {
            output.writeNodes(node);
          });
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [new WrapCommand(EditorCommandId.Blockquote, ['Mod->'], schema.nodes.blockquote)];
  },

  inputRules: (schema: Schema) => {
    return [wrappingInputRule(/^\s*>\s$/, schema.nodes.blockquote)];
  },
};

export default extension;
