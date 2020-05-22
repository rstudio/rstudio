/*
 * emoji.ts
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

import { Schema, Mark, Fragment, Node as ProsemirrorNode } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { EditorState, Transaction } from 'prosemirror-state';


import { Extension } from '../../api/extension';
import { PandocOutput, PandocToken, PandocTokenType, ProsemirrorWriter, PandocExtensions } from '../../api/pandoc';
import { pandocAttrReadAST } from '../../api/pandoc_attr';
import { fragmentText } from '../../api/fragment';

// https://github.com/jgm/emojis/blob/master/emoji.json
interface Emoji {
  emoji: string;
  aliases: string[];
}
import * as kEmojis from './emojis.json';
const kEmojiKeys = Object.keys(kEmojis).filter(key => key !== "default");
const kEmojiChars = kEmojiKeys.map(key => kEmojis[key].emoji);

import { FixupContext } from '../../api/fixup';
import { MarkTransaction } from '../../api/transaction';
import { mergedTextNodes } from '../../api/text';

const kEmojiAttr = 0;
const kEmojiContent = 1;

const extension = (pandocExtensions: PandocExtensions) : Extension | null => {

  if (!pandocExtensions.emoji) {
    return null;
  }

  return {
    marks: [
      {
        name: 'emoji',
        spec: {
          inclusive: false,
          noInputRules: true,
          attrs: {
            emojihint: {}
          },
          parseDOM: [
            {
              tag: "span[class*='emoji']",
              getAttrs(dom: Node | string) {
                const el = dom as Element;
                return {
                  emojihint: el.getAttribute('data-emojihint'),
                };
              },
            },
          ],
          toDOM(mark: Mark) {
            return ['span', { 
              class: 'emoji', 
              title: ':' + mark.attrs.emojihint + ':', 
              'data-emojihint': mark.attrs.emojihint 
            }];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.Span,
              match: (tok: PandocToken) => {
                const attrs = pandocAttrReadAST(tok, kEmojiAttr);
                return attrs.keyvalue.length > 0 && attrs.keyvalue[0][0] === 'data-emoji';
              },
              handler: (schema: Schema) => (writer: ProsemirrorWriter, tok: PandocToken) => {
                const attrs = pandocAttrReadAST(tok, kEmojiAttr);
                const emojihint = attrs.keyvalue[0][1];
                const emojiMark = schema.marks.emoji.create({ emojihint });
                writer.openMark(emojiMark);
                const emojiChar = tok.c[kEmojiContent][0].c;
                writer.writeText(emojiChar);
                writer.closeMark(emojiMark);
              },
            },
          ],
          writer: {
            priority: 16,
            write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
              // look for a matching emjoi
              const char = fragmentText(parent);
              const emoji = emojiFromChar(char);
              if (emoji) {
                output.writeToken(PandocTokenType.Span, () => {
                  // resolve which alias to use
                  let alias =  emoji.aliases[0];
                  if (emoji.aliases.length > 1) {
                    if (emoji.aliases.includes(mark.attrs.emojihint)) {
                      alias = mark.attrs.emojihint;
                    }
                  } 
                  output.writeAttr('', ['emoji'], [['data-emoji', alias]]);
                  output.writeArray(() => {
                    output.writeInlines(parent);
                  });
                });
              } else {
                output.writeInlines(parent);
              }
            },
          },
        },
      },
    ],

    inputRules: () => {
      return [
        new InputRule(/:(\w+):$/, (state: EditorState, match: string[], start: number, end: number) => {
          const emoji = emojiFromAlias(match[1]);
          if (emoji) {
            const tr = state.tr;
            tr.delete(start, end);
            tr.insertText(emoji.emoji);
            return tr;
          } else {
            return null;
          }
        })
      ];
    },

    fixups: (schema: Schema) => {
      return [
        (tr: Transaction, context: FixupContext) => {
          
          // only apply on save
          if (context !== FixupContext.Save) {
            return tr;
          }
  
          // create mark transation wrapper
          const markTr = new MarkTransaction(tr);
  
          const textNodes = mergedTextNodes(markTr.doc, (_node: ProsemirrorNode, parentNode: ProsemirrorNode) =>
            parentNode.type.allowsMarkType(schema.marks.emoji),
          );

          textNodes.forEach(textNode => {
            kEmojiChars.forEach(emojiChar => {
              const charLoc = textNode.text.indexOf(emojiChar);
              if (charLoc !== -1) {
                const start = textNode.pos + charLoc;
                const emoji = emojiFromChar(emojiChar)!;
                const mark = schema.marks.emoji.create( { emojihint: emoji.aliases[0]} );
                markTr.addMark(start, start + emojiChar.length, mark);
              }
            });            
          });

          return tr;
        },
      ];
    },
  };
};



function emojiFromChar(emojiChar: string) : Emoji | null {
  const emojiKey = kEmojiKeys.find(key => kEmojis[key].emoji === emojiChar);
  if (emojiKey) {
    return kEmojis[emojiKey] as Emoji;
  } else {
    return null;
  }
}

function emojiFromAlias(emojiAlias: string) : Emoji | null {
  const emojiKey = kEmojiKeys.find(key => {
    const emoji = kEmojis[key];
    if (emoji) {
      return (emoji as Emoji).aliases.some(alias => alias === emojiAlias);
    } else {
      return false;
    }
   
  });
  if (emojiKey) {
    return kEmojis[emojiKey];
  } else {
    return null;
  }
}

export default extension;
