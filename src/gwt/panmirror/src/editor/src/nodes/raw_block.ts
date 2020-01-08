import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';

import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { findParentNodeOfType, setTextSelection } from 'prosemirror-utils';

import { Extension } from '../api/extension';

import { PandocOutput, PandocToken, PandocTokenType, PandocExtensions } from '../api/pandoc';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';

import { canInsertNode } from '../api/node';
import { EditorUI, RawFormatResult } from '../api/ui';

const RAW_BLOCK_FORMAT = 0;
const RAW_BLOCK_CONTENT = 1;

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
              { class: 'raw-block pm-code-block pm-markup-text-color', 'data-format': node.attrs.format },
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
              getAttrs: (tok: PandocToken) => ({
                format: tok.c[RAW_BLOCK_FORMAT],
              }),
              getText: (tok: PandocToken) => tok.c[RAW_BLOCK_CONTENT],
            },
          ],
          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.RawBlock, () => {
              output.write(node.attrs.format);
              output.write(node.textContent);
            });
          },
        },
      },
    ],

    commands: (_schema: Schema, ui: EditorUI) => {
      if (pandocExtensions.raw_attribute) {
        return [new RawBlockCommand(ui)];
      } else {
        return [];
      }
    },
  };
};

class RawBlockCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI) {
    super(
      EditorCommandId.RawBlock,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        // enable if we are either insie a raw block or we can insert a raw block
        const rawBlock = findParentNodeOfType(schema.nodes.raw_block)(state.selection);
        if (!rawBlock && !canInsertNode(state, schema.nodes.raw_block)) {
          return false;
        }

        async function asyncEditRawBlock() {
          // function to create the node
          function createRawNode(result: RawFormatResult) {
            const rawText = result!.raw.content ? schema.text(result!.raw.content) : undefined;
            return schema.nodes.raw_block.createAndFill({ format: result!.raw.format }, rawText)!;
          }

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
                  tr.replaceRangeWith(range.from, range.to, createRawNode(result));
                  setTextSelection(tr.selection.from - 1, -1)(tr);
                }
              } else {
                tr.replaceSelectionWith(createRawNode(result));
                setTextSelection(tr.mapping.map(state.selection.from), -1)(tr);
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

export default extension;
