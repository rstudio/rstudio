/*
 * div.ts
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

import { Node as ProsemirrorNode, Schema, DOMOutputSpec, ResolvedPos } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { findParentNodeOfType, ContentNodeWithPos } from 'prosemirror-utils';
import { wrapIn, lift } from 'prosemirror-commands';
import { GapCursor } from 'prosemirror-gapcursor';

import { ExtensionContext } from '../api/extension';
import {
  pandocAttrSpec,
  pandocAttrToDomAttr,
  pandocAttrParseDom,
  pandocAttrReadAST,
  pandocAttrFrom,
  pandocAttrAvailable,
  PandocAttr,
} from '../api/pandoc_attr';
import { PandocOutput, PandocTokenType, PandocToken } from '../api/pandoc';
import { ProsemirrorCommand, EditorCommandId, toggleWrap } from '../api/command';
import { EditorUI } from '../api/ui';
import { OmniInsertGroup, OmniInsert } from '../api/omni_insert';
import { markIsActive } from '../api/mark';
import { BaseKey } from '../api/basekeys';
import { attrInputToProps } from '../api/ui-dialogs';

import './div-styles.css';

const DIV_ATTR = 0;
const DIV_CHILDREN = 1;

const extension = (context: ExtensionContext) => {
  const { pandocExtensions, ui } = context;

  if (!pandocExtensions.fenced_divs && !pandocExtensions.native_divs) {
    return null;
  }

  return {
    nodes: [
      {
        name: 'div',
        spec: {
          attrs: {
            ...pandocAttrSpec,
          },
          defining: true,
          content: 'block+',
          group: 'block list_item_block',
          parseDOM: [
            {
              tag: 'div[data-div="1"]',
              getAttrs(dom: Node | string) {
                const attrs: {} = { 'data-div': 1 };
                return {
                  ...attrs,
                  ...pandocAttrParseDom(dom as Element, attrs),
                };
              },
            },
          ],
          toDOM(node: ProsemirrorNode): DOMOutputSpec {
            const attr = {
              'data-div': '1',
              ...pandocAttrToDomAttr({
                ...node.attrs,
                classes: [...node.attrs.classes, 'pm-div', 'pm-div-background-color'],
              }),
            };
            return ['div', attr, 0];
          },
        },

        attr_edit: () => ({
          type: (schema: Schema) => schema.nodes.div,
          editFn: () => divCommand(ui, true),
        }),

        pandoc: {
          readers: [
            {
              token: PandocTokenType.Div,
              block: 'div',
              getAttrs: (tok: PandocToken) => ({
                ...pandocAttrReadAST(tok, DIV_ATTR),
              }),
              getChildren: (tok: PandocToken) => tok.c[DIV_CHILDREN],
            },
          ],
          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.Div, () => {
              output.writeAttr(node.attrs.id, node.attrs.classes, node.attrs.keyvalue);
              output.writeArray(() => {
                output.writeNodes(node);
              });
            });
          },
        },
      },
    ],

    baseKeys: () => {
      return [
        { key: BaseKey.Enter, command: divInputRuleEnter() },
        { key: BaseKey.ArrowLeft, command: arrowHandler('left') },
        { key: BaseKey.ArrowUp, command: arrowHandler('up') },
      ];

    },

    commands: () => {
      return [
        // turn current block into a div
        new DivCommand(EditorCommandId.Div, ui, true),

        // insert a div
        new DivCommand(EditorCommandId.InsertDiv, ui, false, {
          name: ui.context.translateText('Div...'),
          description: ui.context.translateText('Block containing other content'),
          group: OmniInsertGroup.Blocks,
          priority: 1,
          image: () => (ui.prefs.darkMode() ? ui.images.omni_insert?.div_dark! : ui.images.omni_insert?.div!),
        }),
      ];
    },
  };
};

function arrowHandler(_dir: 'up' | 'left') {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {

    // only applies within divs
    const div = findParentNodeOfType(state.schema.nodes.div)(state.selection);
    if (!div) {
      return false;
    }

    // if we are at the top of the document then create a gap cursor
    const $pos = state.doc.resolve(div.pos);
    if (!$pos.nodeBefore && $pos.depth === 1) {
      if (dispatch) {
        const gapCursor = new GapCursor($pos, $pos);
        const tr = state.tr;
        tr.setSelection(gapCursor);
        dispatch(tr);
      }
      return true;
    }

    return false;
  };
}

function divCommand(ui: EditorUI, allowEdit: boolean) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
    // two different modes:
    //  - editing attributes of an existing div
    //  - wrapping (a la blockquote)
    const schema = state.schema;
    const div = allowEdit ? findParentNodeOfType(schema.nodes.div)(state.selection) : undefined;
    if (!div && !toggleWrap(schema.nodes.div)(state)) {
      return false;
    }

    async function asyncEditDiv() {
      if (dispatch) {
        // selecting nothing or entire div means edit, selecting text outside of a
        // div or a subset of an existing div means create new one
        const editMode = div && (state.selection.empty || isFullDivSelection(div, state));
        if (editMode) {
          await editDiv(ui, state, dispatch, div!);
        } else {
          await createDiv(ui, state, dispatch);
        }
        if (view) {
          view.focus();
        }
      }
    }
    asyncEditDiv();

    return true;
  };
}

class DivCommand extends ProsemirrorCommand {
  constructor(id: EditorCommandId, ui: EditorUI, allowEdit: boolean, omniInsert?: OmniInsert) {
    super(id, [], divCommand(ui, allowEdit), omniInsert);
  }
}

async function editDiv(ui: EditorUI, state: EditorState, dispatch: (tr: Transaction) => void, div: ContentNodeWithPos) {
  const attr = pandocAttrFrom(div.node.attrs);
  const result = await ui.dialogs.editDiv(attr, pandocAttrAvailable(attr));
  if (result) {
    const tr = state.tr;
    if (result.action === 'edit') {
      tr.setNodeMarkup(div.pos, div.node.type, result.attr);
      dispatch(tr);
    } else if (result.action === 'remove') {
      lift(state, dispatch);
    }
  }
}

async function createDiv(ui: EditorUI, state: EditorState, dispatch: (tr: Transaction) => void) {
  const result = await ui.dialogs.editDiv({}, false);
  if (result) {
    wrapIn(state.schema.nodes.div)(state, (tr: Transaction) => {
      const div = findParentNodeOfType(state.schema.nodes.div)(tr.selection)!;
      tr.setNodeMarkup(div.pos, div.node.type, result.attr);
      dispatch(tr);
    });
  }
}

function isFullDivSelection(div: ContentNodeWithPos, state: EditorState) {
  const divStart = div.pos;
  const divEnd = div.pos + div.node.nodeSize;
  return state.selection.from - 2 === divStart && state.selection.to + 2 === divEnd;
}

function divInputRuleEnter() {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {
    // see if the parent consist of a pending code block input rule
    const schema = state.schema;


    // selection must be empty
    if (!state.selection.empty) {
      return false;
    }

    // full text of parent must meet the pattern
    // eslint-disable-next-line no-useless-escape
    const match = state.selection.$head.parent.textContent.match(/^:{3,}(\s+({.*?}|\S+)?[\s:]*)?$/);
    if (!match) {
      return false;
    }

    // no inline code marks
    if (markIsActive(state, schema.marks.code)) {
      return false;
    }

    // must be able to perform the replacement
    if (!canApplyDivInputRule(state)) {
      return false;
    }

    // execute
    if (dispatch) {

      // if it's just followed by whitespace then don't do it
      if (match[1] && match[1].trim().length === 0) {
        return false;
      }

      // parse attributes
      const attrs: PandocAttr = pandocAttrFrom({});
      const attribMatch = match[2];
      if (attribMatch) {
        const bracesMatch = attribMatch.match(/^{(.*?)}$/);
        if (bracesMatch) {
          const pandocAttrsText = bracesMatch[1];
          const pandocAttrsMatch = pandocAttrsText.match(/^\s*(#\S+)?\s*((?:\.\S+\s*)*)?(.*)?$/);
          if (pandocAttrsMatch) {
            const attrProps = attrInputToProps({ id: pandocAttrsMatch[1], classes: pandocAttrsMatch[2] });
            attrs.id = attrProps.id || '';
            attrs.classes = attrProps.classes || [];
          }
        } else {
          attrs.classes = [attribMatch];
        }
      }

      wrapIn(state.schema.nodes.div)(state, (tr: Transaction) => {
        const div = findParentNodeOfType(state.schema.nodes.div)(tr.selection)!;
        tr.setNodeMarkup(div.pos, div.node.type, attrs);
        const $head = tr.selection.$head;
        const start = $head.start();
        const end = start + $head.parent.textContent.length;
        tr.deleteRange(start, end);
        dispatch(tr);
      });

    }

    return true;
  };
}

function canReplaceNodeWithDiv(schema: Schema, $pos: ResolvedPos) {
  return $pos.node(-1).canReplaceWith($pos.index(-1), $pos.indexAfter(-1), schema.nodes.div);
}

function canApplyDivInputRule(state: EditorState) {
  const schema = state.schema;
  const { $head } = state.selection;
  return canReplaceNodeWithDiv(schema, $head);
}


export default extension;
