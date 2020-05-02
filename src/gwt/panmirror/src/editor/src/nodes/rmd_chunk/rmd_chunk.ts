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

import { Node as ProsemirrorNode, Schema, NodeType } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { setTextSelection, findParentNodeOfType } from 'prosemirror-utils';

import { Extension } from '../../api/extension';
import { EditorOptions } from '../../api/options';
import { PandocOutput, PandocTokenType, PandocExtensions } from '../../api/pandoc';
import { codeNodeSpec } from '../../api/code';
import { ProsemirrorCommand, EditorCommandId, toggleBlockType } from '../../api/command';
import { selectionIsBodyTopLevel } from '../../api/selection';
import { uuidv4 } from '../../api/util';
import { precedingListItemInsertPos, precedingListItemInsert } from '../../api/list';

import { EditorUI } from '../../api/ui';
import { PandocCapabilities } from '../../api/pandoc_capabilities';
import { EditorFormat, kBookdownDocType } from '../../api/format';

import { RmdChunkImagePreviewPlugin } from './rmd_chunk-image';

import './rmd_chunk-styles.css';

const kRmdCodeChunkClass = '3759D6F8-53AF-4931-8060-E55AF73236B5'.toLowerCase();

const extension = (
  _pandocExtensions: PandocExtensions,
  _pandocCapabilities: PandocCapabilities,
  ui: EditorUI,
  format: EditorFormat,
  options: EditorOptions,
): Extension | null => {
  if (!format.rmdExtensions.codeChunks) {
    return null;
  }

  return {
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
          lineNumbers: true,
          lineNumberFormatter: (line: number) => {
            if (line === 1) {
              return '';
            } else {
              return line - 1 + '';
            }
          },
          bookdownTheorems: format.docTypes.includes(kBookdownDocType),
          classes: ['pm-chunk-background-color'],
          lang: (_node: ProsemirrorNode, content: string) => {
            const match = content.match(/^\{([a-zA-Z0-9_])+/);
            if (match) {
              return match[1];
            } else {
              return null;
            }
          },
        },

        pandoc: {
          codeBlockFilter: {
            preprocessor: (markdown: string) => {
              const md = markdown.replace(
                /^([\t >]*```+)\s*(\{[a-zA-Z0-9_]+( *[ ,].*?)?\})(\s*)([\W\w]*?)(?:```)(?:[ \t]*)$/gm,
                (_match: string, p1: string, p2: string, _p3: string, p4: string, p5: string, p6: string) => {
                  return p1 + kRmdCodeChunkClass + '\n' + p2 + '\n' + p5 + '```\n';
                },
              );
              return md;
            },
            class: kRmdCodeChunkClass,
            nodeType: (schema: Schema) => schema.nodes.rmd_chunk,
            getAttrs: () => ({ navigation_id: uuidv4() }),
          },

          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.Para, () => {
              // split text content into first and subsequent lines
              const lines = node.textContent.split('\n');
              if (lines.length > 0) {
                const first = lines[0].replace(/^.*?\{([^}]*)\}.*?$/, '$1');
                output.writeRawMarkdown('```{' + first + '}\n' + lines.slice(1).join('\n') + '\n```\n');
              }
            });
          },
        },
      },
    ],

    commands: (_schema: Schema) => {
      return [new RmdChunkCommand()];
    },

    plugins: (_schema: Schema) => {
      if (options.rmdImagePreview) {
        return [new RmdChunkImagePreviewPlugin(ui.context)];
      } else {
        return [];
      }
    },
  };
};

class RmdChunkCommand extends ProsemirrorCommand {
  constructor() {
    super(
      EditorCommandId.RmdChunk,
      ['Mod-Alt-i'],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        if (
          !toggleBlockType(schema.nodes.rmd_chunk, schema.nodes.paragraph)(state) &&
          !precedingListItemInsertPos(state.doc, state.selection)
        ) {
          return false;
        }

        // must either be at the body top level, within a list item, or within a
        // blockquote (and never within a table)
        const within = (nodeType: NodeType) => !!findParentNodeOfType(nodeType)(state.selection);
        if (within(schema.nodes.table)) {
          return false;
        }
        if (
          !selectionIsBodyTopLevel(state.selection) &&
          !within(schema.nodes.list_item) &&
          !within(schema.nodes.blockquote)
        ) {
          return false;
        }

        // create chunk text
        if (dispatch) {
          const tr = state.tr;
          const kRmdText = 'r\n';
          const rmdText = schema.text(kRmdText);
          const rmdNode = schema.nodes.rmd_chunk.create({}, rmdText);
          const prevListItemPos = precedingListItemInsertPos(tr.doc, tr.selection);
          if (prevListItemPos) {
            precedingListItemInsert(tr, prevListItemPos, rmdNode);
          } else {
            tr.replaceSelectionWith(rmdNode);
            setTextSelection(tr.mapping.map(state.selection.from) - 1)(tr);
          }
          dispatch(tr);
        }

        return true;
      },
    );
  }
}

export default extension;
