/*
 * raw_inline.ts
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

import { Schema, Node as ProsemirrorNode, Mark, Fragment } from 'prosemirror-model';
import { Plugin, PluginKey } from 'prosemirror-state';
import { findChildrenByMark } from 'prosemirror-utils';
import { DecorationSet, Decoration } from 'prosemirror-view';

import { Extension } from '../../api/extension';
import { ProsemirrorCommand } from '../../api/command';
import { PandocOutput, PandocToken, PandocTokenType, PandocExtensions } from '../../api/pandoc';
import { mergedTextNodes } from '../../api/text';
import { getMarkRange } from '../../api/mark';
import { EditorUI } from '../../api/ui';
import { MarkTransaction } from '../../api/transaction';
import { markHighlightPlugin, markHighlightDecorations } from '../../api/mark-highlight';

import { InsertInlineLatexCommand, RawInlineCommand } from './raw_inline-commands';

import './raw_inline-styles.css';

const RAW_INLINE_FORMAT = 0;
const RAW_INLINE_CONTENT = 1;

const TEX_FORMAT = 'tex';
const HTML_FORMAT = 'html';

const kBeginTex = /(^|[^\\])\\[A-Za-z]/;
const kBeginHTML = /(^|[^\\])</;
const kHTMLComment = /^<!--([\s\S]*?)-->$/;

const extension = (pandocExtensions: PandocExtensions): Extension | null => {
  // short circuit to no extension if none of the raw format bits are set
  if (!pandocExtensions.raw_tex && !pandocExtensions.raw_html && !pandocExtensions.raw_attribute) {
    return null;
  }

  // return the extension
  return {
    marks: [
      {
        name: 'raw_inline',
        spec: {
          inclusive: false,
          excludes: '_',
          attrs: {
            format: {},
            comment: { default: false },
          },
          parseDOM: [
            {
              tag: "span[class*='raw-inline']",
              getAttrs(dom: Node | string) {
                const el = dom as Element;
                return {
                  format: el.getAttribute('data-format'),
                  comment: el.getAttribute('data-comment') === '1',
                };
              },
            },
          ],
          toDOM(mark: Mark) {
            const attr: any = {
              class: 'raw-inline pm-fixedwidth-font pm-markup-text-color',
              'data-format': mark.attrs.format,
              'data-comment': mark.attrs.comment ? '1' : '0',
            };
            return ['span', attr];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.RawInline,
              mark: 'raw_inline',
              getAttrs: (tok: PandocToken) => {
                const text = tok.c[RAW_INLINE_CONTENT];
                const comment = kHTMLComment.test(text);
                return {
                  format: tok.c[RAW_INLINE_FORMAT],
                  comment,
                };
              },
              getText: (tok: PandocToken) => {
                return tok.c[RAW_INLINE_CONTENT];
              },
            },
          ],
          writer: {
            priority: 20,
            write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
              // get raw content
              let raw = '';
              parent.forEach((node: ProsemirrorNode) => (raw = raw + node.textContent));

              // write straight through (bypassing the pandoc ast) for raw html and tex as they will
              // otherwise acquire an explicit raw attribute which is bad for round-tripping, see
              // https://github.com/jgm/pandoc/commit/28cad165179378369fcf4d25656ea28357026baa
              if ([TEX_FORMAT, HTML_FORMAT].includes(mark.attrs.format)) {
                output.writeRawMarkdown(raw);
              } else {
                output.writeToken(PandocTokenType.RawInline, () => {
                  output.write(mark.attrs.format);
                  output.write(raw);
                });
              }
            },
          },
        },
      },
    ],

    // insert command
    commands: (_schema: Schema, ui: EditorUI) => {
      const commands: ProsemirrorCommand[] = [];

      if (pandocExtensions.raw_attribute) {
        commands.push(new RawInlineCommand(ui));
      }

      if (pandocExtensions.raw_tex) {
        commands.push(new InsertInlineLatexCommand());
      }

      return commands;
    },

    appendMarkTransaction: (schema: Schema) => {
      return [
        {
          name: 'raw-inline-marks',
          filter: node => node.isTextblock && node.type.allowsMarkType(schema.marks.raw_inline),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            // short circuit to do nothing if neither raw html or raw tex are enabled
            if (!pandocExtensions.raw_tex && !pandocExtensions.raw_html) {
              return;
            }

            const rawInlineMark = (node: ProsemirrorNode) => schema.marks.raw_inline.isInSet(node.marks);
            const hasRawInlineContent = (pattern: RegExp) => {
              return (node: ProsemirrorNode, parentNode: ProsemirrorNode) => {
                return (
                  node.isText &&
                  !schema.marks.code.isInSet(node.marks) &&
                  !node.marks.some(mark => mark.type.excludes(schema.marks.raw_inline)) &&
                  parentNode.type.allowsMarkType(schema.marks.raw_inline) &&
                  pattern.test(parentNode.textContent)
                );
              };
            };

            // remove marks from inline nodes as needed
            const rawInlineNodes = findChildrenByMark(node, schema.marks.raw_inline, true);
            rawInlineNodes.forEach(rawInlineNode => {
              const node = rawInlineNode.node;
              const mark = rawInlineMark(node);
              if (mark) {
                const from = pos + 1 + rawInlineNode.pos;
                const rawInlineRange = getMarkRange(tr.doc.resolve(from), schema.marks.raw_inline);
                if (rawInlineRange) {
                  const text = tr.doc.textBetween(rawInlineRange.from, rawInlineRange.to);
                  const preceding = tr.doc.textBetween(rawInlineRange.from - 1, rawInlineRange.from);
                  const removeInvalidedMark = (markupLength: (text: string) => number) => {
                    const length = markupLength(text);
                    if (preceding === '\\') {
                      tr.removeMark(rawInlineRange.from, rawInlineRange.to, schema.marks.raw_inline);
                    } else if (length !== text.length) {
                      tr.removeMark(rawInlineRange.from + length, rawInlineRange.to, schema.marks.raw_inline);
                    }
                  };
                  if (pandocExtensions.raw_tex && mark.attrs.format === TEX_FORMAT) {
                    removeInvalidedMark(texLength);
                  }
                  if (pandocExtensions.raw_html && mark.attrs.format === HTML_FORMAT) {
                    removeInvalidedMark(htmlLength);
                  }
                }
              }
            });

            const addRawInlineMarks = (
              format: string,
              beginMarkup: RegExp,
              markupLength: (text: string) => number,
              commentRegex?: RegExp,
            ) => {
              const searchForMarkup = (text: string) => {
                const match = text.match(beginMarkup);
                if (match && match.index !== undefined) {
                  return match.index + match[1].length;
                } else {
                  return -1;
                }
              };

              const hasMarkup = hasRawInlineContent(beginMarkup);
              const markupNodes = mergedTextNodes(node, hasMarkup);
              markupNodes.forEach(markupNode => {
                const text = markupNode.text;
                let beginIdx = searchForMarkup(text);
                while (beginIdx !== -1) {
                  const length = markupLength(text.substring(beginIdx));
                  if (length > 0) {
                    const from = pos + 1 + markupNode.pos + beginIdx;
                    const to = from + length;
                    const markRange = getMarkRange(tr.doc.resolve(markupNode.pos + beginIdx), schema.marks.raw_inline);
                    if (!markRange || markRange.to !== to) {
                      let comment = commentRegex ? commentRegex.test(tr.doc.textBetween(from, to)) : false;
                      const mark = schema.mark('raw_inline', { format, comment });
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
            };
            // this is adding back the tex mark we escaped away
            if (pandocExtensions.raw_tex) {
              addRawInlineMarks(TEX_FORMAT, kBeginTex, texLength);
            }
            if (pandocExtensions.raw_html) {
              addRawInlineMarks(HTML_FORMAT, kBeginHTML, htmlLength, kHTMLComment);
            }
          },
        },
      ];
    },

    // plugin to add and remove raw_inline latex marks as the user edits
    plugins: (schema: Schema) => {
      const plugins: Plugin[] = [];
      plugins.push(rawInlineHighlightPlugin(schema));
      return plugins;
    },
  };
};

const key = new PluginKey<DecorationSet>('latex-highlight');

export function rawInlineHighlightPlugin(schema: Schema) {
  const kLightTextClass = 'pm-light-text-color';
  const delimiterRegex = /[{}]/g;

  return markHighlightPlugin(key, schema.marks.raw_inline, (text, attrs, markRange) => {
    if (attrs.format === TEX_FORMAT) {
      const kIdClass = 'pm-markup-text-color';
      const idRegEx = /\\[A-Za-z]+/g;
      let decorations = markHighlightDecorations(markRange, text, idRegEx, kIdClass);
      decorations = decorations.concat(markHighlightDecorations(markRange, text, delimiterRegex, kLightTextClass));
      return decorations;
    } else if (attrs.format === HTML_FORMAT && attrs.comment) {
      return [Decoration.inline(markRange.from, markRange.to, { class: kLightTextClass })];
    } else {
      return [];
    }
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

function htmlLength(text: string) {
  let inSingleQuote = false;
  let inDoubleQuote = false;
  let i;
  for (i = 0; i < text.length; i++) {
    // next character
    const ch = text[i];

    // must start with <[/]{str}
    if (i === 0 && ch !== '<') {
      return 0;
    }
    if (i === 1 && !isLetter(ch) && ch !== '!' && ch !== '/') {
      return 0;
    }

    // invalid if we see another < when not in quotes
    if (i > 0 && ch === '<' && !inSingleQuote && !inDoubleQuote) {
      return 0;
    }

    // > terminates if we aren't in quotes
    if (ch === '>' && !inSingleQuote && !inDoubleQuote) {
      return i + 1;
    }

    // handle single quote
    if (ch === "'") {
      if (inSingleQuote) {
        inSingleQuote = false;
      } else if (!inDoubleQuote) {
        inSingleQuote = true;
      }

      // handle double quote
    } else if (ch === '"') {
      if (inDoubleQuote) {
        inDoubleQuote = false;
      } else if (!inSingleQuote) {
        inDoubleQuote = true;
      }
    }
  }

  return 0;
}

export default extension;
