/*
 * rmd_chunk.ts
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

import { Extension } from '../../api/extension';
import { EditorOptions } from '../../api/options';
import { PandocOutput, PandocTokenType, PandocExtensions } from '../../api/pandoc';

import { codeNodeSpec } from '../../api/code';
import { ProsemirrorCommand, EditorCommandId } from '../../api/command';

import { EditorUI } from '../../api/ui';
import { PandocCapabilities } from '../../api/pandoc_capabilities';
import { EditorFormat, kBookdownDocType } from '../../api/format';
import { rmdChunk, EditorRmdChunk, insertRmdChunk } from '../../api/rmd';
import { OmniInsertGroup } from '../../api/omni_insert';

import { RmdChunkImagePreviewPlugin } from './rmd_chunk-image';
import { ExecuteCurrentRmdChunkCommand, ExecutePreviousRmdChunksCommand } from './rmd_chunk-commands';
import { rmdChunkBlockCapsuleFilter } from './rmd_chunk-capsule';

import './rmd_chunk-styles.css';

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
          lineNumberFormatter: (lineNumber: number, lineCount?: number, line?: string) => {
            if (lineNumber === 1) {
              return '';
            } else {
              return lineNumber - 1 + '';
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
            : undefined,
        },

        pandoc: {
          blockCapsuleFilter: rmdChunkBlockCapsuleFilter(),

          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.Para, () => {
              const parts = rmdChunk(node.textContent);
              if (parts) {
                const code = parts.code ? parts.code + '\n' : '';
                output.writeRawMarkdown('```{' + parts.meta + '}\n' + code + '```\n');
              }
            });
          },
        },
      },
    ],

    commands: (_schema: Schema) => {
      const commands = [
        new RChunkCommand(ui),
        new PythonChunkCommand(ui),
        new BashChunkCommand(ui),
        new RcppChunkCommand(ui),
        new SQLChunkCommand(ui),
        new D3ChunkCommand(ui),
        new StanChunkCommand(ui),
      ];
      if (ui.execute.executeRmdChunk) {
        commands.push(new ExecuteCurrentRmdChunkCommand(ui), new ExecutePreviousRmdChunksCommand(ui));
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
  constructor(
    ui: EditorUI,
    id: EditorCommandId,
    keymap: string[],
    priority: number,
    lang: string,
    placeholder: string,
    rowOffset = 1,
    colOffset = 0,
    selectionOffset?: number,
  ) {
    super(id, keymap, insertRmdChunk(placeholder, rowOffset, colOffset), {
      name: `${lang} ${ui.context.translateText('Code Chunk')}`,
      description: `${ui.context.translateText('Executable')} ${lang} ${ui.context.translateText('chunk')}`,
      group: OmniInsertGroup.Chunks,
      priority,
      selectionOffset: selectionOffset || colOffset || placeholder.length,
      image: () => ui.images.omni_insert?.generic!,
    });
  }
}

class RChunkCommand extends RmdChunkCommand {
  constructor(ui: EditorUI) {
    super(ui, EditorCommandId.RCodeChunk, ['Mod-Alt-i'], 10, 'R', '{r}\n');
  }
}

class PythonChunkCommand extends RmdChunkCommand {
  constructor(ui: EditorUI) {
    super(ui, EditorCommandId.PythonCodeChunk, [], 8, 'Python', '{python}\n');
  }
}

class BashChunkCommand extends RmdChunkCommand {
  constructor(ui: EditorUI) {
    super(ui, EditorCommandId.BashCodeChunk, [], 7, 'Bash', '{bash}\n');
  }
}

class RcppChunkCommand extends RmdChunkCommand {
  constructor(ui: EditorUI) {
    super(ui, EditorCommandId.RcppCodeChunk, [], 6, 'Rcpp', '{Rcpp}\n');
  }
}

class SQLChunkCommand extends RmdChunkCommand {
  constructor(ui: EditorUI) {
    super(ui, EditorCommandId.SQLCodeChunk, [], 5, 'SQL', '{sql connection=}\n', 0, 16);
  }
}

class D3ChunkCommand extends RmdChunkCommand {
  constructor(ui: EditorUI) {
    super(ui, EditorCommandId.D3CodeChunk, [], 4, 'D3', '{d3 data=}\n', 0, 9);
  }
}

class StanChunkCommand extends RmdChunkCommand {
  constructor(ui: EditorUI) {
    super(ui, EditorCommandId.StanCodeChunk, [], 7, 'Stan', '{stan output.var=}\n', 0, 17);
  }
}

export default extension;
