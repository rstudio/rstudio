/*
 * raw_block.ts
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

import { Node as ProsemirrorNode, Schema, NodeType } from 'prosemirror-model';

import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { setBlockType } from 'prosemirror-commands';

import { findParentNode } from 'prosemirror-utils';

import { Extension, ExtensionContext } from '../api/extension';

import {
  PandocOutput,
  PandocToken,
  PandocTokenType,
  ProsemirrorWriter,
  kRawBlockContent,
  kRawBlockFormat,
} from '../api/pandoc';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';

import { EditorUI } from '../api/ui';
import { isSingleLineHTML } from '../api/html';
import { kHTMLFormat, kTexFormat, editRawBlockCommand, isRawHTMLFormat } from '../api/raw';
import { isSingleLineTex } from '../api/tex';
import { OmniInsert, OmniInsertGroup } from '../api/omni_insert';

const extension = (context: ExtensionContext): Extension | null => {
  const { pandocExtensions, pandocCapabilities, ui } = context;

  const rawAttribute = pandocExtensions.raw_attribute;

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
          attrEditFn: rawAttribute ? editRawBlockCommand(ui, pandocCapabilities.output_formats) : undefined,
          borderColorClass: 'pm-raw-block-border',
        },

        attr_edit: () => ({
          type: (schema: Schema) => schema.nodes.raw_block,
          tags: (node: ProsemirrorNode) => [node.attrs.format],
          editFn: rawAttribute
            ? () => editRawBlockCommand(ui, pandocCapabilities.output_formats)
            : () => (state: EditorState) => false,
        }),

        pandoc: {
          readers: [
            {
              token: PandocTokenType.RawBlock,
              block: 'raw_block',
            },
          ],

          // ensure that usethis badges comment ends up in it's own block
          preprocessor: (markdown: string) => {
            return markdown.replace(/([^\n])(\n^<!-- badges: end -->$)/gm, (_match, p1, p2) => {
              return p1 + "\n" + p2;
            });
          },

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
            if (!pandocExtensions.raw_attribute || node.textContent.trim() === '<!-- -->') {
              output.writeToken(PandocTokenType.Para, () => {
                output.writeRawMarkdown(node.textContent);
              });
            } else {
              output.writeToken(PandocTokenType.RawBlock, () => {
                output.write(node.attrs.format);
                output.write(node.textContent);
              });
            }
          },
        },
      },
    ],

    commands: (schema: Schema) => {
      const commands: ProsemirrorCommand[] = [];

      commands.push(
        new FormatRawBlockCommand(EditorCommandId.HTMLBlock, kHTMLFormat, schema.nodes.raw_block, {
          name: ui.context.translateText('HTML Block'),
          description: ui.context.translateText('Raw HTML content'),
          group: OmniInsertGroup.Blocks,
          priority: 6,
          image: () =>
            ui.prefs.darkMode() ? ui.images.omni_insert?.html_block_dark! : ui.images.omni_insert?.html_block!,
        }),
      );

      if (pandocExtensions.raw_tex) {
        commands.push(
          new FormatRawBlockCommand(EditorCommandId.TexBlock, kTexFormat, schema.nodes.raw_block, {
            name: ui.context.translateText('TeX Block'),
            description: ui.context.translateText('Raw TeX content'),
            group: OmniInsertGroup.Blocks,
            priority: 5,
            image: () =>
              ui.prefs.darkMode() ? ui.images.omni_insert?.tex_block_dark! : ui.images.omni_insert?.tex_block!,
          }),
        );
      }

      if (rawAttribute) {
        commands.push(new RawBlockCommand(ui, pandocCapabilities.output_formats));
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
  const textTrimmed = text.trimRight();
  if (isRawHTMLFormat(format) && isSingleLineHTML(textTrimmed) && writer.hasInlineHTMLWriter(textTrimmed)) {
    writer.openNode(schema.nodes.paragraph, {});
    writer.writeInlineHTML(textTrimmed);
    writer.closeNode();

    // similarly, single lines of tex should be read as inline tex
  } else if (format === kTexFormat && isSingleLineTex(textTrimmed)) {
    writer.openNode(schema.nodes.paragraph, {});
    const rawTexMark = schema.marks.raw_tex.create();
    writer.openMark(rawTexMark);
    writer.writeText(textTrimmed);
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

  constructor(id: EditorCommandId, format: string, nodeType: NodeType, omniInsert?: OmniInsert) {
    super(
      id,
      [],
      (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
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
      },
      omniInsert,
    );
    this.format = format;
    this.nodeType = nodeType;
  }

  public isActive(state: EditorState) {
    return !!findParentNode(node => node.type === this.nodeType && node.attrs.format === this.format)(state.selection);
  }
}

// generic raw block command (shows dialog to allow choosing from among raw formats)
class RawBlockCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI, outputFormats: string[]) {
    super(EditorCommandId.RawBlock, [], editRawBlockCommand(ui, outputFormats), {
      name: ui.context.translateText('Raw Block...'),
      description: ui.context.translateText('Raw content block'),
      group: OmniInsertGroup.Blocks,
      priority: 4,
      image: () => (ui.prefs.darkMode() ? ui.images.omni_insert?.raw_block_dark! : ui.images.omni_insert?.raw_block!),
    });
  }
}

export default extension;
