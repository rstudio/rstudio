/*
 * navigation.ts
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

import { EditorView } from 'prosemirror-view';
import { setTextSelection, Predicate, findChildren } from 'prosemirror-utils';

import zenscroll from 'zenscroll';
import { editingRootNode } from './node';

export function navigateTo(view: EditorView, predicate: Predicate, animate = true) {
  const result = findChildren(view.state.doc, predicate);
  if (result.length) {
    navigateToPosition(view, result[0].pos, animate);
  }
}

export function navigateToId(view: EditorView, id: string, animate = true) {
  navigateTo(view, node => node.attrs.id === id, animate);
}

export function navigateToHeading(view: EditorView, heading: string, animate = true) {
  navigateTo(
    view,
    node => {
      return (
        node.type === view.state.schema.nodes.heading &&
        node.textContent.localeCompare(heading, undefined, { sensitivity: 'accent' }) === 0
      );
    },
    animate,
  );
}

export function navigateToPosition(view: EditorView, pos: number, animate = true) {
  // set selection
  view.dispatch(setTextSelection(pos)(view.state.tr));

  // scroll to selection
  const node = view.nodeDOM(pos);
  if (node instanceof HTMLElement) {
    const editingRoot = editingRootNode(view.state.selection)!;
    const container = view.nodeDOM(editingRoot.pos) as HTMLElement;
    const scroller = zenscroll.createScroller(container, 700, 20);
    if (animate) {
      scroller.to(node);
    } else {
      scroller.to(node, 0);
    }
  }
}
