/*
 * rmd_chunk.ts
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

import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { setTextSelection } from 'prosemirror-utils';

import { Extension } from '../../api/extension';
import { EditorOptions } from '../../api/options';
import { PandocOutput, PandocTokenType, PandocExtensions } from '../../api/pandoc';
import { codeNodeSpec } from '../../api/code';
import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';
import { selectionIsBodyTopLevel } from '../../api/selection';
import { uuidv4 } from '../../api/util';

import './rmd_chunk-styles.css';

const kRmdCodeChunkClass = 'D34DA053-95B6-4F12-B665-6CA8E4CD5101';

const extension: Extension = {
  nodes: [
    {
      name: 'rmd_chunk',
      spec: {
        ...codeNodeSpec(),
        attrs: {
          navigation_id: { default: null },
        },
        parseDOM: [
          {
            tag: "div[class*='rmd-chunk']",
            preserveWhitespace: 'full',
          },
        ],
        toDOM(node: ProsemirrorNode) {
          return ['div', { class: 'rmd-chunk pm-code-block' }, 0];
        },
      },

      code_view: {
        firstLineMeta: true,
        classes: ['pm-chunk-background-color'],
        lang: (_node: ProsemirrorNode, content: string) => {
          const match = content.match(/^[a-zA-Z0-9_]+/);
          if (match) {
            return match[0];
          } else {
            return null;
          }
        },
      },

      pandoc: {
        codeBlockFilter: {
          preprocessor: (markdown: string) => {
            const md = markdown.replace(
              /^(```+\s*\{)([a-zA-Z0-9_]+( *[ ,].*?)?)(\}\s*)([\W\w]*?)(?:```)(?:[ \t]*)$/gm,
              function(_match: string, p1: string, p2: string, _p3: string, p4: string, p5: string, p6: string) {
                return p1 + '.' + kRmdCodeChunkClass + '}\n' + p2 + '\n' + p5 + '```\n';
              },
            );
            return md;
          },
          class: kRmdCodeChunkClass,
          nodeType: schema => schema.nodes.rmd_chunk,
          getAttrs: () => ({ navigation_id: uuidv4() }),
        },

        writer: (output: PandocOutput, node: ProsemirrorNode) => {
          output.writeToken(PandocTokenType.Para, () => {
            // split text content into first and subsequent lines
            const lines = node.textContent.split('\n');
            if (lines.length > 0) {
              output.writeRawMarkdown('```{' + lines[0] + '}\n' + lines.slice(1).join('\n') + '\n```\n');
            }
          });
        },
      },
    },
  ],

  commands: (_schema: Schema) => {
    return [new RmdChunkCommand()];
  },
};

class RmdChunkCommand extends ProsemirrorCommand {
  constructor() {
    super(
      EditorCommandId.RmdChunk,
      ['Shift-Mod-u'],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        if (!canInsertNode(state, schema.nodes.rmd_chunk)) {
          return false;
        }

        // only allow inserting at the top level
        if (!selectionIsBodyTopLevel(state.selection)) {
          return false;
        }

        // create yaml metadata text
        if (dispatch) {
          const tr = state.tr;
          const kRmdText = 'r\n';
          const rmdText = schema.text(kRmdText);
          const rmdNode = schema.nodes.rmd_chunk.create({}, rmdText);
          const prevSel = tr.selection.from;
          tr.replaceSelectionWith(rmdNode);
          setTextSelection(prevSel + kRmdText.length, -1)(tr);
          dispatch(tr);
        }

        return true;
      },
    );
  }
}

export default (pandocExtensions: PandocExtensions, options: EditorOptions) => {
  if (options.rmdCodeChunks && pandocExtensions.backtick_code_blocks && pandocExtensions.fenced_code_attributes) {
    return extension;
  } else {
    return null;
  }
};
