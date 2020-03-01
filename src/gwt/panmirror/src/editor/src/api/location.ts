import { EditorView } from 'prosemirror-view';
import { setTextSelection } from 'prosemirror-utils';

import { bodyElement } from './dom';
import { kAddToHistoryTransaction, kRestoreLocationTransaction } from './transaction';

export interface EditingLocation {
  pos: number;
  scrollTop: number;
}

export function getEditingLocation(view: EditorView): EditingLocation {
  const pos = view.state.selection.from;
  const bodyEl = bodyElement(view);
  const scrollTop = bodyEl.scrollTop;
  return { pos, scrollTop };
}

export function restoreEditingLocation(view: EditorView, location: EditingLocation, scrollIntoView = true) {
  // ensure location is valid
  if (location.pos > view.state.doc.nodeSize) {
    return;
  }

  // restore selection
  const tr = view.state.tr;
  setTextSelection(location.pos)(tr)
    .setMeta(kRestoreLocationTransaction, true)
    .setMeta(kAddToHistoryTransaction, false);
  view.dispatch(tr);

  // scroll to selection
  if (scrollIntoView) {
    bodyElement(view).scrollTop = location.scrollTop;
  }
}
