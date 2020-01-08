import { EditorState, Transaction, Selection } from 'prosemirror-state';
import { setTextSelection, findParentNodeOfType } from 'prosemirror-utils';
import { toggleMark } from 'prosemirror-commands';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';

import { MathType, delimiterForType } from './math';

export class InsertInlineMathCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.InlineMath, [], insertMathCommand(MathType.Inline));
  }
}

export class InsertDisplayMathCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.DisplayMath, [], insertMathCommand(MathType.Display));
  }
}

function insertMathCommand(type: MathType) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void) => {
    // enable/disable command
    const schema = state.schema;
    if (!canInsertNode(state, schema.nodes.text) || !toggleMark(schema.marks.math)(state)) {
      return false;
    }

    if (dispatch) {
      const tr = state.tr;
      insertMath(state.selection, type, tr);
      dispatch(tr);
    }
    return true;
  };
}

export function insertMath(selection: Selection, type: MathType, tr: Transaction) {
  // include a newline for display math in an empty paragraph
  const schema = tr.doc.type.schema;
  let content = '';
  if (type === MathType.Display) {
    const para = findParentNodeOfType(schema.nodes.paragraph)(selection);
    if (para && !para.node.textContent.length) {
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
