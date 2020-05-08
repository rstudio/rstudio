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
import { PandocOutput, PandocTokenType, PandocExtensions, ProsemirrorWriter, PandocToken } from '../../api/pandoc';
import { pandocAttrReadAST, PandocAttr } from '../../api/pandoc_attr';
import { PandocBlockCapsule, parsePandocBlockCapsule } from '../../api/pandoc_capsule';

import { codeNodeSpec } from '../../api/code';
import { ProsemirrorCommand, EditorCommandId, toggleBlockType } from '../../api/command';
import { selectionIsBodyTopLevel } from '../../api/selection';
import { uuidv4 } from '../../api/util';
import { precedingListItemInsertPos, precedingListItemInsert } from '../../api/list';

import { EditorUI } from '../../api/ui';
import { PandocCapabilities } from '../../api/pandoc_capabilities';
import { EditorFormat, kBookdownDocType } from '../../api/format';
import { rmdChunk, EditorRmdChunk } from '../../api/rmd';
import { EditorEvents } from '../../api/events';

import { RmdChunkImagePreviewPlugin } from './rmd_chunk-image';
import { ExecuteCurrentRmdChunkCommand, ExecutePreviousRmdChunksCommand } from './rmd_chunk-commands';

import './rmd_chunk-styles.css';

const extension = (
  _pandocExtensions: PandocExtensions,
  _pandocCapabilities: PandocCapabilities,
  ui: EditorUI,
  format: EditorFormat,
  options: EditorOptions,
  events: EditorEvents
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
            const match = content.match(/^\{([a-zA-Z0-9_]+)/);
            if (match) {
              return match[1];
            } else {
              return null;
            }
          },
          executeRmdChunkFn: ui.execute.executeRmdChunk 
            ? (chunk: EditorRmdChunk) => ui.execute.executeRmdChunk!(chunk)
            : undefined
        },

        pandoc: {

          blockCapsuleFilter: rmdChunkBlockCapsuleFilter(),

          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.Para, () => {
              const parts = rmdChunk(node.textContent);
              if (parts) {
                output.writeRawMarkdown('```{' + parts.meta + '}\n' + parts.code + '\n```\n');
              }
            });
          },
        },
      },
    ],

    commands: (_schema: Schema) => {
      const commands = [new RmdChunkCommand()];
      if (ui.execute.executeRmdChunk) {
        commands.push(
          new ExecuteCurrentRmdChunkCommand(ui),
          new ExecutePreviousRmdChunksCommand(ui)
        );
      }
      return commands;
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
          const kRmdText = '{r}\n';
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

function rmdChunkBlockCapsuleFilter() {

  const kBlockCapsuleType = 'F3175F2A-E8A0-4436-BE12-B33925B6D220'.toLowerCase();
  const kBlockCapsuleTextRegEx = new RegExp('```' + kBlockCapsuleType + '\\n[ \\t>]*([^`]+)\\n[ \\t>]*```', 'g');

  return {

    type: kBlockCapsuleType,
    
    match: /^([\t >]*)(```+\s*\{[a-zA-Z0-9_]+(?: *[ ,].*?)?\}[ \t]*\n[\W\w]*?(?:\n[\t >]*```+|[\t >]*```+))([ \t]*)$/gm,
    
    // textually enclose the capsule so that pandoc parses it as the type of block we want it to
    // (in this case a code block). we use the capsule prefix here to make sure that the code block's
    // content and end backticks match the indentation level of the first line correctly
    enclose: (capsuleText: string, capsule: PandocBlockCapsule) => 
      '```' + kBlockCapsuleType + '\n' + 
      capsule.prefix + capsuleText + '\n' +
      capsule.prefix + '```'
    ,

    // look for one of our block capsules within pandoc ast text (e.g. a code or raw block)
    // and if we find it, parse and return the original source code
    handleText: (text: string) => {
      return text.replace(kBlockCapsuleTextRegEx, (_match, p1) => {
        const capsuleText = p1;
        const capsule = parsePandocBlockCapsule(capsuleText);
        return capsule.source;
      });
    },
    
    // look for a block capsule of our type within a code block (indicated by the 
    // presence of a special css class)
    handleToken: (tok: PandocToken) => {
      if (tok.t === PandocTokenType.CodeBlock) {
        const attr = pandocAttrReadAST(tok, 0);
        if ((attr as PandocAttr).classes.includes(kBlockCapsuleType)) {
          return tok.c[1];
        }
      }
      return null;
    },

    // write the node as an rmd_chunk, being careful to remove the backticks 
    // preserved as part of the source, and stripping out the base indentation
    // level implied by the prefix
    writeNode: (schema: Schema, writer: ProsemirrorWriter, capsule: PandocBlockCapsule) => {

      // open node
      writer.openNode(schema.nodes.rmd_chunk, { navigation_id: uuidv4() });

      // source still has leading and trailing backticks, remove them
      const source = capsule.source.replace(/^```+/, '').replace(/\n[\t >]*```+$/, '');

      // prefix represents the indentation level of the block's source code, strip that
      // same prefix from all the lines of code save for the first one
      const prefixStripRegEx = new RegExp('^' + capsule.prefix);
      const lines = source.split('\n').map((line, index) => {
        return index > 0 ? line.replace(prefixStripRegEx, '') : line;
      });

      // write the lines
      writer.writeText(lines.join('\n'));

      // all done
      writer.closeNode();
    }
  };
}

export default extension;
