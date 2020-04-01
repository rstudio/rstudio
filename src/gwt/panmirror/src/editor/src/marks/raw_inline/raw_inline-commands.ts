/*
 * raw_inline-commands.ts
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

import { EditorState, Transaction } from 'prosemirror-state';
import { toggleMark } from 'prosemirror-commands';
import { EditorView } from 'prosemirror-view';

import { setTextSelection } from 'prosemirror-utils';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';
import { EditorUI, RawFormatProps } from '../../api/ui';
import { getMarkAttrs, markIsActive, getMarkRange } from '../../api/mark';
import { kHTMLFormat } from '../../api/raw';

export class RawInlineCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI) {
    super(
      EditorCommandId.RawInline,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        if (!canInsertInlineRaw(state)) {
          return false;
        }

        async function asyncInlineRaw() {
          if (dispatch) {
            // check if mark is active
            const isActive = markIsActive(state, schema.marks.raw_inline);

            // get the range of the mark
            let range = { from: state.selection.from, to: state.selection.to };
            if (isActive) {
              range = getMarkRange(state.selection.$from, schema.marks.raw_inline) as { from: number; to: number };
            }

            // get raw attributes if we have them
            let raw: RawFormatProps = { content: '', format: '' };
            raw.content = state.doc.textBetween(range.from, range.to);
            if (isActive) {
              raw = {
                ...raw,
                ...getMarkAttrs(state.doc, state.selection, schema.marks.raw_inline),
              };
            }

            const result = await ui.dialogs.editRawInline(raw);
            if (result) {
              const tr = state.tr;
              tr.removeMark(range.from, range.to, schema.marks.raw_inline);
              if (result.action === 'edit') {
                const mark = schema.marks.raw_inline.create({ format: result.raw.format });
                const node = schema.text(result.raw.content, [mark]);
                // if we are editing a selection then replace it, otherwise insert
                if (raw.content) {
                  tr.replaceRangeWith(range.from, range.to, node);
                } else {
                  tr.replaceSelectionWith(node, false);
                }
              }
              dispatch(tr);
            }

            if (view) {
              view.focus();
            }
          }
        }
        asyncInlineRaw();

        return true;
      },
    );
  }
}

export class InsertInlineLatexCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.TexInline, [], (state: EditorState, dispatch?: (tr: Transaction) => void) => {
      if (!canInsertInlineRaw(state)) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;
        const node = state.schema.text('\\');
        tr.replaceSelectionWith(node);
        dispatch(tr);
      }
      return true;
    });
  }
}

export class InsertInlineHTMLCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.HTMLInline, [], (state: EditorState, dispatch?: (tr: Transaction) => void) => {
      if (!canInsertInlineRaw(state)) {
        return false;
      }

      if (dispatch) {
        const tr = state.tr;
        const schema = state.schema;
        const mark = schema.marks.raw_inline.create( { format: kHTMLFormat });
        const node = state.schema.text('<>', [mark]);
        tr.replaceSelectionWith(node, false);
        setTextSelection(tr.selection.to-1)(tr);
        dispatch(tr);
      }
      return true;
    });
  }
}

function canInsertInlineRaw(state: EditorState) {
  const schema = state.schema;
  return canInsertNode(state, schema.nodes.text) && toggleMark(schema.marks.raw_inline)(state);
}
