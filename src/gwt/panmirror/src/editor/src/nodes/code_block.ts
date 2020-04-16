/*
 * code_block.ts
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

import { Node as ProsemirrorNode, Schema, ResolvedPos } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { newlineInCode, exitCode } from 'prosemirror-commands';
import { EditorState, Transaction, TextSelection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { findParentNodeOfType, setTextSelection } from 'prosemirror-utils';

import { BlockCommand, EditorCommandId, ProsemirrorCommand } from '../api/command';
import { Extension } from '../api/extension';
import { BaseKey } from '../api/basekeys';
import { codeNodeSpec } from '../api/code';
import { PandocOutput, PandocTokenType, PandocExtensions } from '../api/pandoc';
import { pandocAttrSpec, pandocAttrParseDom, pandocAttrToDomAttr, pandocAttrFrom } from '../api/pandoc_attr';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorUI, CodeBlockProps, EditorUIContext } from '../api/ui';
import { canInsertNode } from '../api/node';
import { markIsActive } from '../api/mark';

const extension = (
  pandocExtensions: PandocExtensions, 
  pandocCapabilities: PandocCapabilities, 
  ui: EditorUI): Extension => 
{
  const hasAttr = hasFencedCodeAttributes(pandocExtensions);

  return {
    nodes: [
      {
        name: 'code_block',

        spec: {
          ...codeNodeSpec(),
          attrs: { ...(hasAttr ? pandocAttrSpec : {}) },
          parseDOM: [
            {
              tag: 'pre',
              preserveWhitespace: 'full',
              getAttrs: (node: Node | string) => {
                if (hasAttr) {
                  const el = node as Element;
                  return pandocAttrParseDom(el, {});
                } else {
                  return {};
                }
              },
            },
          ],
          toDOM(node: ProsemirrorNode) {
            const fontClass = 'pm-fixedwidth-font';
            const attrs = hasAttr
              ? pandocAttrToDomAttr({
                  ...node.attrs,
                  classes: [...node.attrs.classes, fontClass],
                })
              : {
                  class: fontClass,
                };
            return ['pre', attrs, ['code', 0]];
          },
        },

        code_view: {
          lang: (node: ProsemirrorNode) => {
            if (node.attrs.classes && node.attrs.classes.length) {
              return node.attrs.classes[0];
            } else {
              return null;
            }
          },
        },

        attr_edit: codeBlockAttrEdit(pandocExtensions, pandocCapabilities, ui),

        pandoc: {
          readers: [
            {
              token: PandocTokenType.CodeBlock,
              code_block: true,
            },
          ],
          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.CodeBlock, () => {
              if (hasAttr) {
                output.writeAttr(node.attrs.id, node.attrs.classes, node.attrs.keyvalue);
              } else {
                output.writeAttr();
              }
              output.write(node.textContent);
            });
          },
        },
      },
    ],

    commands: (schema: Schema) => {
      const commands: ProsemirrorCommand[] = [
        new BlockCommand(
          EditorCommandId.CodeBlock,
          ['Shift-Ctrl-\\'],
          schema.nodes.code_block,
          schema.nodes.paragraph,
          {},
        ),
      ];
      if (hasAttr) {
        commands.push(
          new CodeBlockFormatCommand(pandocExtensions, ui, pandocCapabilities.highlight_languages)
        );
      }
      return commands;
    },

    baseKeys: () => {
      return [
        { key: BaseKey.Enter, command: newlineInCode },
        { key: BaseKey.Enter, command: codeBlockInputRuleEnter(ui.context) },
        { key: BaseKey.ModEnter, command: exitCode },
        { key: BaseKey.ShiftEnter, command: exitCode },
      ];
    },

    inputRules: (schema: Schema) => {
      return [
        codeBlockInputRule(schema, ui.context)
      ];
    },
  };
};

function codeBlockInputRule(schema: Schema, uiContext: EditorUIContext) {
  return new InputRule(/^```$/, (state: EditorState, match: string[], start: number, end: number) => {

    if (!canReplaceNodeWithCodeBlock(schema, state.selection.$from)) {
      return null;
    }

    const tr = state.tr;
    tr.deleteRange(start, end);
    const langText = langPlaceholderText(uiContext);
    const insertText = match[0] + langText;
    const codeMark = schema.marks.code.create();
    tr.addStoredMark(codeMark);
    tr.insertText(insertText);
    tr.removeStoredMark(codeMark);
    tr.setSelection(TextSelection.create(tr.doc, start + match[0].length, start + insertText.length));
    return tr;
  });
}

function codeBlockInputRuleEnter(uiContext: EditorUIContext) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {
    
    // see if the parent consist of a pending code block input rule
    const { $head } = state.selection;

    // full text of parent must meet the pattern
    const match = $head.parent.textContent.match(/^```(\w*)$/);
    if (!match) {
      return false;
    }

    // must be enclosed in a code mark
    const schema = state.schema;
    if (!markIsActive(state, schema.marks.code)) {
      return false;
    }
    
    // must be able to perform the replacement
    if (!canReplaceNodeWithCodeBlock(schema, $head)) {
      return false;
    }

    // execute
    if (dispatch) {

      // insert the code block
      const tr = state.tr;
      const lang = match[1];
      const attrs = lang.length && lang !== langPlaceholderText(uiContext) 
        ? pandocAttrFrom({ classes: [lang]} ) : {};
      const start = $head.start();
      tr.delete(start, start + $head.parent.textContent.length);
      tr.setBlockType(start, start, schema.nodes.code_block, attrs);
      dispatch(tr);
    }

    return true;
  };
}

function langPlaceholderText(uiContext: EditorUIContext) {
  return uiContext.translateText('lang');
}

function canReplaceNodeWithCodeBlock(schema: Schema, $pos: ResolvedPos) {
  return $pos.node(-1).canReplaceWith($pos.index(-1), $pos.indexAfter(-1), schema.nodes.code_block);
}

class CodeBlockFormatCommand extends ProsemirrorCommand {
  constructor(pandocExtensions: PandocExtensions, ui: EditorUI, languages: string[]) {
    super(
      EditorCommandId.CodeBlockFormat,
      [],
      codeBlockFormatCommandFn(pandocExtensions, ui, languages)
    );
  }
}

function codeBlockFormatCommandFn(pandocExtensions: PandocExtensions, ui: EditorUI, languages: string[]) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {

    // enable if we are either inside a code block or we can insert a code block
    const schema = state.schema;
    const codeBlock = findParentNodeOfType(schema.nodes.code_block)(state.selection);
    if (!codeBlock && !canInsertNode(state, schema.nodes.code_block)) {
      return false;
    }

    async function asyncEditCodeBlock() {
      if (dispatch) {
        
          // get props to edit
          const codeBlockProps = codeBlock 
            ? { ...codeBlock.node.attrs as CodeBlockProps, lang: '' }
            : { id: '', classes: [], keyvalue: [], lang: '' }; 

          // set lang if the first class is from available languages
          // (alternatively if we don't support attributes then it's 
          // automatically considered the language)
          if (codeBlockProps.classes && codeBlockProps.classes.length) {
            const potentialLang = codeBlockProps.classes[0];
            if (!pandocExtensions.fenced_code_attributes || languages.includes(potentialLang)) {
              codeBlockProps.lang = potentialLang;
              codeBlockProps.classes = codeBlockProps.classes.slice(1);
            }
          }

          // show dialog
          const result = await ui.dialogs.editCodeBlock(
            codeBlockProps, 
            pandocExtensions.fenced_code_attributes, 
            languages
          );
          if (result) {

            // extract lang
            const newProps = { ...result };
            if (newProps.classes && newProps.lang) {
              newProps.classes.unshift(result.lang);
            }

            // edit or insert as appropriate
            const tr = state.tr;
            if (codeBlock) {
              tr.setNodeMarkup(codeBlock.pos, schema.nodes.code_block, newProps);
            } else {
              const prevSel = tr.selection;
              tr.replaceSelectionWith(schema.nodes.code_block.create(newProps));
              setTextSelection(tr.mapping.map(prevSel.from), -1)(tr);
            }
            dispatch(tr);
          }
      }

      if (view) {
        view.focus();
      }
    }

    asyncEditCodeBlock();

    return true;
  };
}

function codeBlockAttrEdit(
  pandocExtensions: PandocExtensions, 
  pandocCapabilities: PandocCapabilities,
  ui: EditorUI
) {
  return () => {
    if (hasFencedCodeAttributes(pandocExtensions)) {
      return {
        type: (schema: Schema) => schema.nodes.code_block,
        tags: (node: ProsemirrorNode) => {
          const tags: string[] = [];
          if (node.attrs.id) {
            tags.push(`#${node.attrs.id}`);
          }
          if (node.attrs.classes && node.attrs.classes.length) {
            const lang = node.attrs.classes[0];
            if (pandocCapabilities.highlight_languages.includes(lang)) {
              tags.push(lang);
            } else {
              tags.push(`.${lang}`);
            }
          } 
          return tags;
        },
        editFn: () => codeBlockFormatCommandFn(
          pandocExtensions, 
          ui, 
          pandocCapabilities.highlight_languages
        )
      };
    } else {
      return null;
    }
  };
}

function hasFencedCodeAttributes(pandocExtensions: PandocExtensions) {
  return pandocExtensions.fenced_code_blocks || 
         pandocExtensions.fenced_code_attributes;
}


export default extension;
