/*
 * raw_tex.ts
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

import { Node as ProsemirrorNode, Mark, Fragment, Schema } from "prosemirror-model";
import { DecorationSet } from "prosemirror-view";
import { Plugin, PluginKey, EditorState, Transaction, Selection, TextSelection } from "prosemirror-state";
import { toggleMark } from "prosemirror-commands";
import { InputRule, textblockTypeInputRule } from "prosemirror-inputrules";

import { findChildrenByMark, setTextSelection } from "prosemirror-utils";

import { PandocExtensions, PandocToken, PandocTokenType, PandocOutput } from "../../api/pandoc";
import { Extension } from "../../api/extension";
import { kTexFormat } from "../../api/raw";
import { EditorUI } from "../../api/ui";
import { markHighlightPlugin, markHighlightDecorations } from "../../api/mark-highlight";
import { MarkTransaction } from "../../api/transaction";
import { getMarkRange, markIsActive } from "../../api/mark";
import { mergedTextNodes } from "../../api/text";
import { ProsemirrorCommand, EditorCommandId } from "../../api/command";
import { canInsertNode } from "../../api/node";

import { kRawInlineFormat, kRawInlineContent } from './raw_inline';
import { texLength } from "../../api/tex";

const kBeginTex = /(^|[^\\])\\[A-Za-z]/;

const extension = (pandocExtensions: PandocExtensions): Extension | null => {

  if (!pandocExtensions.raw_tex) {
    return null;
  }

  return {
    marks: [
      {
        name: 'raw_tex',
        noInputRules: true,
        spec: {
          inclusive: true,
          excludes: '_',
          attrs: {},
          parseDOM: [
            {
              tag: "span[class*='raw-tex']"
            },
          ],
          toDOM(_mark: Mark) {
            const attr: any = {
              class: 'raw-tex pm-fixedwidth-font',
            };
            return ['span', attr];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.RawInline,
              mark: 'raw_tex',
              match: (tok: PandocToken) => {
                const format = tok.c[kRawInlineFormat];
                return format === kTexFormat;
              },
              getText: (tok: PandocToken) => {
                return tok.c[kRawInlineContent];
              }
            },
          ],
          writer: {
            priority: 20,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              output.writeRawMarkdown(parent);
            },
          },
        },
      },
    ],

    // insert command
    commands: (_schema: Schema, ui: EditorUI) => {
      return [new InsertInlineLatexCommand()];
    },

    appendMarkTransaction: (schema: Schema) => {
      return [
        {
          name: 'remove-raw-tex-marks',
          filter: node => node.isTextblock && node.type.allowsMarkType(schema.marks.raw_tex),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
    
            // remove marks from raw_tex as needed
            const rawTexMark = (nd: ProsemirrorNode) => schema.marks.raw_tex.isInSet(nd.marks);
            const rawTexNodes = findChildrenByMark(node, schema.marks.raw_tex, true);
            rawTexNodes.forEach(rawTexNode => {
              const mark = rawTexMark(rawTexNode.node);
              if (mark) {
                const from = pos + 1 + rawTexNode.pos;
                const rawInlineRange = getMarkRange(tr.doc.resolve(from), schema.marks.raw_tex);
                if (rawInlineRange) {
                  const text = tr.doc.textBetween(rawInlineRange.from, rawInlineRange.to);
                  const length = texLength(text, rawInlineRange.from, tr.selection);
                  if (length > -1 && length !== text.length) {
                    tr.removeMark(rawInlineRange.from + length, rawInlineRange.to, schema.marks.raw_tex);
                  }
                }
              }
            });
          },
        },
      ];
    },

    inputRules: (schema: Schema) => {
      return [
        texInputRule(schema)
      ];
    },

    // plugin to add highlighting decorations
    plugins: (schema: Schema) => {
      const plugins: Plugin[] = [];
      plugins.push(latexHighlightingPlugin(schema));
      return plugins;
    },

  };

};



function texInputRule(schema: Schema) {
  return new InputRule(/(^| )\\$/, (state: EditorState, match: string[], start: number, end: number) => {
    
    const rawTexMark = schema.marks.raw_tex;
    
    if (state.selection.empty && toggleMark(rawTexMark)(state)) {
      
      // create transaction
      const tr = state.tr;
      
      // insert tex backslash
      const mark = schema.marks.raw_tex.create();
      tr.addStoredMark(mark);
      tr.insertText('\\');
      
      // extend the mark to cover any valid tex that immediately follows the \
      let extended = false;
      const { parent, parentOffset } = tr.selection.$head;
      const text = parent.textContent.slice(parentOffset - 1);
      if (text.length > 0) {
        const length = texLength(text, tr.selection.from - 1, tr.selection);
        if (length > 1) {
          const startTex = tr.selection.from - 1;
          tr.addMark(startTex, startTex + length, mark);
          extended = true;
        }
      } 

      // if it wasn't extended then insert/select placeholder
      if (!extended) {
        const placeholder = 'tex';
        tr.insertText(placeholder);
        tr.setSelection(new TextSelection(
          tr.doc.resolve(tr.selection.from - placeholder.length), 
          tr.doc.resolve(tr.selection.from)
        ));
      }

      return tr;
    } else {
      return null;
    }
  });
}


class InsertInlineLatexCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.TexInline, [], (state: EditorState, dispatch?: (tr: Transaction) => void) => {
      const schema = state.schema;
      if (!canInsertNode(state, schema.nodes.text) || !toggleMark(schema.marks.raw_tex)(state)) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;
        const mark = schema.marks.raw_tex.create();
        const node = state.schema.text('\\', [mark]);
        tr.replaceSelectionWith(node, false);
        dispatch(tr);
      }
      return true;
    });
  }
}



const key = new PluginKey<DecorationSet>('latex-highlight');

export function latexHighlightingPlugin(schema: Schema) {
  const kLightTextClass = 'pm-light-text-color';
  const delimiterRegex = /[{}]/g;

  return markHighlightPlugin(key, schema.marks.raw_tex, (text, _attrs, markRange) => {
    const kIdClass = 'pm-markup-text-color';
    const idRegEx = /\\[A-Za-z]+/g;
    let decorations = markHighlightDecorations(markRange, text, idRegEx, kIdClass);
    decorations = decorations.concat(markHighlightDecorations(markRange, text, delimiterRegex, kLightTextClass));
    return decorations;
  });
}


export default extension;

