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
import { Plugin, PluginKey, EditorState, Transaction } from "prosemirror-state";
import { toggleMark } from "prosemirror-commands";

import { findChildrenByMark } from "prosemirror-utils";

import { PandocExtensions, PandocToken, PandocTokenType, PandocOutput } from "../../api/pandoc";
import { Extension } from "../../api/extension";
import { kTexFormat } from "../../api/raw";
import { EditorUI } from "../../api/ui";
import { markHighlightPlugin, markHighlightDecorations } from "../../api/mark-highlight";
import { MarkTransaction } from "../../api/transaction";
import { getMarkRange } from "../../api/mark";
import { mergedTextNodes } from "../../api/text";
import { ProsemirrorCommand, EditorCommandId } from "../../api/command";
import { canInsertNode } from "../../api/node";

import { kRawInlineFormat, kRawInlineContent } from './raw_inline';

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
          inclusive: false,
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
          name: 'raw-tex-marks',
          filter: node => node.isTextblock && node.type.allowsMarkType(schema.marks.raw_tex),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
          
            const rawTexMark = (nd: ProsemirrorNode) => schema.marks.raw_tex.isInSet(nd.marks);
            const hasRawTexContent = (pattern: RegExp) => {
              return (nd: ProsemirrorNode, parentNode: ProsemirrorNode) => {
                return (
                  nd.isText &&
                  !schema.marks.code.isInSet(nd.marks) &&
                  (!schema.marks.math || !schema.marks.math.isInSet(nd.marks)) &&
                  parentNode.type.allowsMarkType(schema.marks.raw_tex) &&
                  pattern.test(parentNode.textContent)
                );
              };
            };

            // remove marks from inline nodes as needed
            const rawTexNodes = findChildrenByMark(node, schema.marks.raw_tex, true);
            rawTexNodes.forEach(rawTexNode => {
              const mark = rawTexMark(rawTexNode.node);
              if (mark) {
                const from = pos + 1 + rawTexNode.pos;
                const rawInlineRange = getMarkRange(tr.doc.resolve(from), schema.marks.raw_tex);
                if (rawInlineRange) {
                  const text = tr.doc.textBetween(rawInlineRange.from, rawInlineRange.to);
                  const preceding = tr.doc.textBetween(rawInlineRange.from - 1, rawInlineRange.from);
                  const removeInvalidedMark = (markupLength: (text: string) => number) => {
                    const length = markupLength(text);
                    if (preceding === '\\') {
                      tr.removeMark(rawInlineRange.from, rawInlineRange.to, schema.marks.raw_tex);
                    } else if (length !== text.length) {
                      tr.removeMark(rawInlineRange.from + length, rawInlineRange.to, schema.marks.raw_tex);
                    }
                  };
                  removeInvalidedMark(texLength);
                }
              }
            });

            const searchForMarkup = (text: string) => {
              const match = text.match(kBeginTex);
              if (match && match.index !== undefined) {
                return match.index + match[1].length;
              } else {
                return -1;
              }
            };

            const hasMarkup = hasRawTexContent(kBeginTex);
            const markupNodes = mergedTextNodes(node, hasMarkup);
            markupNodes.forEach(markupNode => {
              const text = markupNode.text;
              let beginIdx = searchForMarkup(text);
              while (beginIdx !== -1) {
                const length = texLength(text.substring(beginIdx));
                if (length > 0) {
                  const from = pos + 1 + markupNode.pos + beginIdx;
                  const to = from + length;
                  const markRange = getMarkRange(tr.doc.resolve(markupNode.pos + beginIdx), schema.marks.raw_tex);
                  if (!markRange || markRange.to !== to) {
                    const mark = schema.mark('raw_tex');
                    tr.addMark(from, to, mark);
                  }
                }
                const nextSearchIdx = length ? beginIdx + length : beginIdx + 1;
                beginIdx = searchForMarkup(text.slice(nextSearchIdx));
                if (beginIdx !== -1) {
                  beginIdx = nextSearchIdx + beginIdx;
                }
              }
            });
          },
        },
      ];
    },

    // plugin to add and remove raw_tex latex marks as the user edits
    plugins: (schema: Schema) => {
      const plugins: Plugin[] = [];
      plugins.push(rawInlineHighlightPlugin(schema));
      return plugins;
    },

  };

};

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
        tr.replaceSelectionWith(node);
        dispatch(tr);
      }
      return true;
    });
  }
}



const key = new PluginKey<DecorationSet>('latex-highlight');

export function rawInlineHighlightPlugin(schema: Schema) {
  const kLightTextClass = 'pm-light-text-color';
  const delimiterRegex = /[{}]/g;

  return markHighlightPlugin(key, schema.marks.raw_tex, (text, attrs, markRange) => {
    const kIdClass = 'pm-markup-text-color';
    const idRegEx = /\\[A-Za-z]+/g;
    let decorations = markHighlightDecorations(markRange, text, idRegEx, kIdClass);
    decorations = decorations.concat(markHighlightDecorations(markRange, text, delimiterRegex, kLightTextClass));
    return decorations;
  });
}

const LetterRegex = /[A-Za-z]/;
function isLetter(ch: string) {
  return LetterRegex.test(ch);
}

function texLength(text: string) {
  // reject entirely if we start with \\
  if (text.startsWith('\\\\')) {
    return 0;
  }

  let braceLevel = 0;
  let i;
  for (i = 0; i < text.length; i++) {
    // next character
    const ch = text[i];

    // must start with \{ltr}
    if (i === 0 && ch !== '\\') {
      return 0;
    }
    if (i === 1 && !isLetter(ch)) {
      return 0;
    }

    // non-letter / non-open-brace if we aren't in braces terminates
    if (i > 0 && !isLetter(ch) && ch !== '{' && braceLevel <= 0) {
      return i;
    }

    // manage brace levels
    if (ch === '{') {
      braceLevel++;
    } else if (ch === '}') {
      braceLevel--;
    }
  }

  if (braceLevel === 0) {
    return i;
  } else {
    return 0;
  }
}


export default extension;

