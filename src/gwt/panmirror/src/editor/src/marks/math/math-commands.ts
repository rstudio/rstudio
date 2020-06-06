/*
 * math-commands.ts
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

import { EditorState, Transaction, Selection } from 'prosemirror-state';
import { setTextSelection, findParentNodeOfType } from 'prosemirror-utils';
import { toggleMark } from 'prosemirror-commands';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';

import { MathType, delimiterForType } from './math';
import { Schema } from 'prosemirror-model';
import { OmniInserter } from '../../api/omni_insert';
import { EditorUIImages } from '../../api/ui-images';
import { EditorUI } from '../../api/ui';

export class InsertInlineMathCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.InlineMath, [], insertMathCommand(MathType.Inline, false));
  }
}

export class InsertDisplayMathCommand extends ProsemirrorCommand {
  constructor(allowNewline: boolean) {
    super(EditorCommandId.DisplayMath, [], insertMathCommand(MathType.Display, allowNewline));
  }
}

export function inlineMathOmniInsert(schema: Schema, ui: EditorUI) : OmniInserter {
  return {
    id: EditorCommandId.InlineMath,
    name: ui.context.translateText("Inline Math"),
    keywords: [ui.context.translateText('equation')],
    description: ui.context.translateText("Math included within a line or paragraph"),
    group: ui.context.translateText('Content'),
    image: dark => dark ? ui.images.omni_insert?.math_inline_dark! : ui.images.omni_insert?.math_inline!,
    command: insertMathCommand(MathType.Inline, false)
  };
}

export function displayMathOmniInsert(schema: Schema, ui: EditorUI, allowNewline: boolean) : OmniInserter {
  return {
    id: EditorCommandId.DisplayMath,
    name: ui.context.translateText("Display Math"),
    keywords: [ui.context.translateText('equation')],
    description: ui.context.translateText("Math set apart from the main text"),
    group: ui.context.translateText('Content'),
    image: dark => dark ? ui.images.omni_insert?.math_display_dark! : ui.images.omni_insert?.math_display!,
    command: insertMathCommand(MathType.Display, allowNewline)
  };
}

function insertMathCommand(type: MathType, allowNewline: boolean) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {
    // enable/disable command
    const schema = state.schema;
    if (!canInsertNode(state, schema.nodes.text) || !toggleMark(schema.marks.math)(state)) {
      return false;
    }

    if (dispatch) {
      const tr = state.tr;
      insertMath(state.selection, type, allowNewline, tr);
      dispatch(tr);
    }
    return true;
  };
}

export function insertMath(selection: Selection, type: MathType, allowNewline: boolean, tr: Transaction) {
  // include a newline for display math in an empty paragraph
  const schema = tr.doc.type.schema;
  let content = '';
  if (type === MathType.Display) {
    const para = findParentNodeOfType(schema.nodes.paragraph)(selection);
    if (allowNewline && para && !para.node.textContent.length) {
      content = '\n\n';
    }
  }
  const delim = delimiterForType(type);
  const mathText = schema.text(delim + content + delim);
  tr.replaceSelectionWith(mathText, false);
  const mathMark = schema.marks.math.create({ type });
  const from = tr.selection.head - content.length - delim.length * 2;
  const to = from + delim.length * 2 + content.length;
  tr.addMark(from, to, mathMark);
  const pos = tr.mapping.map(selection.head) - delim.length - (content ? 1 : 0);
  return setTextSelection(pos)(tr).scrollIntoView();
}
