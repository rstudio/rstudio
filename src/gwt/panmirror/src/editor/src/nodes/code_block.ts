/*
 * code_block.ts
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
import { newlineInCode, exitCode } from 'prosemirror-commands';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { findParentNodeOfType } from 'prosemirror-utils';

import { BlockCommand, EditorCommandId, ProsemirrorCommand, toggleBlockType } from '../api/command';
import { Extension, ExtensionContext } from '../api/extension';
import { BaseKey } from '../api/basekeys';
import { codeNodeSpec } from '../api/code';
import { PandocOutput, PandocTokenType, PandocExtensions } from '../api/pandoc';
import { pandocAttrSpec, pandocAttrParseDom, pandocAttrToDomAttr } from '../api/pandoc_attr';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorUI } from '../api/ui';
import { CodeBlockProps } from '../api/ui-dialogs';
import { hasFencedCodeBlocks } from '../api/pandoc_format';
import { precedingListItemInsertPos, precedingListItemInsert } from '../api/list';
import { EditorOptions } from '../api/options';
import { OmniInsertGroup } from '../api/omni_insert';

const extension = (context: ExtensionContext): Extension => {
  const { pandocExtensions, pandocCapabilities, ui, options } = context;

  const hasAttr = hasFencedCodeBlocks(pandocExtensions);

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
            return codeBlockLang(node, options);
          },
          attrEditFn: codeBlockFormatCommandFn(pandocExtensions, ui, pandocCapabilities.highlight_languages),
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
                const id = pandocExtensions.fenced_code_attributes ? node.attrs.id : '';
                const keyvalue = pandocExtensions.fenced_code_attributes ? node.attrs.keyvalue : [];
                output.writeAttr(id, node.attrs.classes, keyvalue);
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
        new BlockCommand(EditorCommandId.CodeBlock, [], schema.nodes.code_block, schema.nodes.paragraph, {}),
      ];
      if (hasAttr) {
        commands.push(new CodeBlockFormatCommand(pandocExtensions, ui, pandocCapabilities.highlight_languages));
      }
      return commands;
    },

    baseKeys: () => {
      return [
        { key: BaseKey.Enter, command: newlineInCode },
        { key: BaseKey.ModEnter, command: exitCode },
        { key: BaseKey.ShiftEnter, command: exitCode },
      ];
    },
  };
};

class CodeBlockFormatCommand extends ProsemirrorCommand {
  constructor(pandocExtensions: PandocExtensions, ui: EditorUI, languages: string[]) {
    super(
      EditorCommandId.CodeBlockFormat,
      ['Shift-Mod-\\'],
      codeBlockFormatCommandFn(pandocExtensions, ui, languages),
      {
        name: ui.context.translateText('Code Block...'),
        description: ui.context.translateText('Source code display'),
        group: OmniInsertGroup.Blocks,
        priority: 7,
        image: () =>
          ui.prefs.darkMode() ? ui.images.omni_insert?.code_block_dark! : ui.images.omni_insert?.code_block!,
      },
    );
  }
}

function codeBlockFormatCommandFn(pandocExtensions: PandocExtensions, ui: EditorUI, languages: string[]) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
    // enable if we are either inside a code block or we can toggle to a code block
    const schema = state.schema;
    const codeBlock = findParentNodeOfType(schema.nodes.code_block)(state.selection);
    if (
      !codeBlock &&
      !toggleBlockType(schema.nodes.code_block, schema.nodes.paragraph)(state) &&
      !precedingListItemInsertPos(state.doc, state.selection)
    ) {
      return false;
    }

    async function asyncEditCodeBlock() {
      if (dispatch) {
        // get props to edit
        const codeBlockProps = codeBlock
          ? { ...(codeBlock.node.attrs as CodeBlockProps), lang: '' }
          : defaultCodeBlockProps();

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
          languages,
        );
        if (result) {
          // extract lang
          const applyProps = propsWithLangClass(result);

          // edit or toggle as appropriate
          if (codeBlock) {
            const tr = state.tr;
            tr.setNodeMarkup(codeBlock.pos, schema.nodes.code_block, applyProps);
            dispatch(tr);
          } else {
            const prevListItemPos = precedingListItemInsertPos(state.doc, state.selection);
            if (prevListItemPos) {
              const tr = state.tr;
              const block = schema.nodes.code_block.create(applyProps);
              precedingListItemInsert(tr, prevListItemPos, block);
              dispatch(tr);
            } else {
              toggleBlockType(schema.nodes.code_block, schema.nodes.paragraph, applyProps)(state, dispatch, view);
            }
          }
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

function defaultCodeBlockProps() {
  return { id: '', classes: [], keyvalue: [], lang: '' };
}

function propsWithLangClass(props: CodeBlockProps) {
  const newProps = { ...props };
  if (newProps.classes && newProps.lang) {
    newProps.classes.unshift(props.lang);
  }
  return newProps;
}

// determine the code block language. if it's an Rmd example (i.e. with `r ''`) and
// we have rmdExampleHighlight enabled then use the Rmd chunk language for highlighting
function codeBlockLang(node: ProsemirrorNode, options: EditorOptions) {
  if (node.attrs.classes && node.attrs.classes.length) {
    const lang = node.attrs.classes[0];
    if (options.rmdExampleHighlight && lang === 'md') {
      const match = node.textContent.match(/^```+\s*\{([a-zA-Z0-9_]+)( *[ ,].*)?\}`r ''`/);
      if (match) {
        return match[1];
      }
    }
    return lang;
  } else {
    return null;
  }
}

function codeBlockAttrEdit(pandocExtensions: PandocExtensions, pandocCapabilities: PandocCapabilities, ui: EditorUI) {
  return () => {
    if (hasFencedCodeBlocks(pandocExtensions)) {
      return {
        type: (schema: Schema) => schema.nodes.code_block,
        tags: (node: ProsemirrorNode) => {
          const tags: string[] = [];
          if (node.attrs.id) {
            tags.push(`#${node.attrs.id}`);
          }
          if (node.attrs.classes && node.attrs.classes.length) {
            const lang = node.attrs.classes[0];
            if (pandocCapabilities.highlight_languages.includes(lang) || lang === 'tex') {
              tags.push(lang);
            } else {
              tags.push(`.${lang}`);
            }
          }
          return tags;
        },
        offset: {
          top: 3,
          right: 0
        },
        editFn: () => codeBlockFormatCommandFn(pandocExtensions, ui, pandocCapabilities.highlight_languages),
      };
    } else {
      return null;
    }
  };
}

export default extension;
