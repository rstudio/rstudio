/*
 * raw_block.ts
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
import { setBlockType } from 'prosemirror-commands';

import { findParentNodeOfType, setTextSelection, findParentNode } from 'prosemirror-utils';

import { Extension } from '../api/extension';

import {
  PandocOutput,
  PandocToken,
  PandocTokenType,
  PandocExtensions,
  ProsemirrorWriter,
  kRawBlockContent,
  kRawBlockFormat,
} from '../api/pandoc';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';

import { canInsertNode } from '../api/node';
import { EditorUI, RawFormatProps } from '../api/ui';
import { isSingleLineHTML } from '../api/html';
import { kHTMLFormat, kTexFormat } from '../api/raw';
import { isSingleLineTex } from '../api/tex';

const extension = (pandocExtensions: PandocExtensions): Extension | null => {
  // requires either raw_attribute or raw_html
  if (!pandocExtensions.raw_attribute && !pandocExtensions.raw_html) {
    return null;
  }

  return {
    nodes: [
      {
        name: 'raw_block',
        spec: {
          content: 'text*',
          group: 'block',
          marks: '',
          code: true,
          defining: true,
          isolating: true,
          attrs: {
            format: {},
          },
          parseDOM: [
            {
              tag: "div[class*='raw-block']",
              preserveWhitespace: 'full',
              getAttrs: (node: Node | string) => {
                const el = node as Element;
                return {
                  format: el.getAttribute('data-format'),
                };
              },
            },
          ],
          toDOM(node: ProsemirrorNode) {
            return [
              'div',
              {
                class: 'raw-block pm-fixedwidth-font pm-code-block pm-markup-text-color',
                'data-format': node.attrs.format,
              },
              0,
            ];
          },
        },

        code_view: {
          lang: (node: ProsemirrorNode) => {
            return node.attrs.format;
          },
        },

        pandoc: {
          readers: [
            {
              token: PandocTokenType.RawBlock,
              block: 'raw_block',
            },
          ],
          // we define a custom blockReader here so that we can convert html and tex blocks with
          // a single line of code into paragraph with a raw inline
          blockReader: (schema: Schema, tok: PandocToken, writer: ProsemirrorWriter) => {
            if (tok.t === PandocTokenType.RawBlock) {
              readPandocRawBlock(schema, tok, writer);
              return true;
            } else {
              return false;
            }
          },
          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.RawBlock, () => {
              output.write(node.attrs.format);
              output.write(node.textContent);
            });
          },
        },
      },
    ],

    commands: (schema: Schema, ui: EditorUI) => {
      const commands: ProsemirrorCommand[] = [];

      if (pandocExtensions.raw_attribute) {
        commands.push(new FormatRawBlockCommand(EditorCommandId.HTMLBlock, kHTMLFormat, schema.nodes.raw_block));
        commands.push(new FormatRawBlockCommand(EditorCommandId.TexBlock, kTexFormat, schema.nodes.raw_block));
        commands.push(new RawBlockCommand(ui));
      }

      return commands;
    },
  };
};

function readPandocRawBlock(schema: Schema, tok: PandocToken, writer: ProsemirrorWriter) {
  // single lines of html should be read as inline html (allows for
  // highlighting and more seamless editing experience)
  const format = tok.c[kRawBlockFormat];
  const text = tok.c[kRawBlockContent] as string;
  if (format === kHTMLFormat && isSingleLineHTML(text)) {
    writer.openNode(schema.nodes.paragraph, {});
    writer.writeInlineHTML(text.trimRight());
    writer.closeNode();

    // similarly, single lines of tex should be read as inline tex
  } else if (format === kTexFormat && isSingleLineTex(text)) {
    writer.openNode(schema.nodes.paragraph, {});
    const rawTexMark = schema.marks.raw_tex.create();
    writer.openMark(rawTexMark);
    writer.writeText(text.trimRight());
    writer.closeMark(rawTexMark);
    writer.closeNode();
  } else {
    writer.openNode(schema.nodes.raw_block, { format });
    writer.writeText(text);
    writer.closeNode();
  }
}

// base class for format specific raw block commands (e.g. html/tex)
class FormatRawBlockCommand extends ProsemirrorCommand {
  private format: string;
  private nodeType: NodeType;

  constructor(id: EditorCommandId, format: string, nodeType: NodeType) {
    super(id, [], (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
      if (!this.isActive(state) && !setBlockType(this.nodeType, { format })(state)) {
        return false;
      }

      if (dispatch) {
        const schema = state.schema;
        if (this.isActive(state)) {
          setBlockType(schema.nodes.paragraph)(state, dispatch);
        } else {
          setBlockType(this.nodeType, { format })(state, dispatch);
        }
      }

      return true;
    });
    this.format = format;
    this.nodeType = nodeType;
  }

  public isActive(state: EditorState) {
    return !!findParentNode(node => node.type === this.nodeType && node.attrs.format === this.format)(state.selection);
  }
}

// generic raw block command (shows dialog to allow choosing from among raw formats)
class RawBlockCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI) {
    super(
      EditorCommandId.RawBlock,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        // enable if we are either inside a raw block or we can insert a raw block
        const rawBlock = findParentNodeOfType(schema.nodes.raw_block)(state.selection);
        if (!rawBlock && !canInsertNode(state, schema.nodes.raw_block)) {
          return false;
        }

        async function asyncEditRawBlock() {
          if (dispatch) {
            // get existing attributes (if any)
            const raw = {
              format: '',
              content: '',
            };
            if (rawBlock) {
              raw.format = rawBlock.node.attrs.format;
              raw.content = state.doc.textBetween(rawBlock.pos, rawBlock.pos + rawBlock.node.nodeSize);
            }

            // show dialog
            const result = await ui.dialogs.editRawBlock(raw);
            if (result) {
              const tr = state.tr;

              // remove means convert the block to text
              if (rawBlock) {
                const range = { from: rawBlock.pos, to: rawBlock.pos + rawBlock.node.nodeSize };
                if (result.action === 'remove') {
                  tr.setBlockType(range.from, range.to, schema.nodes.paragraph);
                } else if (result.action === 'edit') {
                  tr.replaceRangeWith(range.from, range.to, createRawNode(schema, result.raw));
                  setTextSelection(tr.selection.from - 1, -1)(tr);
                }
              } else {
                insertRawNode(tr, result.raw);
              }

              dispatch(tr);
            }
          }

          if (view) {
            view.focus();
          }
        }
        asyncEditRawBlock();

        return true;
      },
    );
  }
}

// function to create a raw node
function createRawNode(schema: Schema, raw: RawFormatProps) {
  const rawText = raw.content ? schema.text(raw.content) : undefined;
  return schema.nodes.raw_block.createAndFill({ format: raw.format }, rawText)!;
}

// function to create and insert a raw node, then set selection inside of it
function insertRawNode(tr: Transaction, raw: RawFormatProps) {
  const schema = tr.doc.type.schema;
  const prevSel = tr.selection;
  tr.replaceSelectionWith(createRawNode(schema, raw));
  setTextSelection(tr.mapping.map(prevSel.from), -1)(tr);
}

export default extension;
