/*
 * scroll.ts
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
import { findParentNodeOfTypeClosestToPos } from 'prosemirror-utils';

import zenscroll from 'zenscroll';

import { editingRootNodeClosestToPos, editingRootNode } from './node';

export function scrollIntoView(
  view: EditorView,
  pos: number,
  center = true,
  duration?: number,
  offset?: number,
  onDone?: VoidFunction,
) {
  // resolve position and determine container
  const $pos = view.state.doc.resolve(pos);
  const container = editingRootNodeClosestToPos($pos);

  // if we have a container then do the scroll
  if (container) {
    const schema = view.state.schema;
    const containerEl = view.nodeDOM(container.pos) as HTMLElement;
    const parentList = findParentNodeOfTypeClosestToPos($pos, [schema.nodes.ordered_list, schema.nodes.bullet_list]);
    const parentDiv = schema.nodes.div ? findParentNodeOfTypeClosestToPos($pos, schema.nodes.div) : undefined;
    let resultPos = $pos.before();
    if (parentList) {
      resultPos = parentList.pos;
    } else if (parentDiv) {
      resultPos = $pos.before(2);
    }
    const resultNode = view.nodeDOM(resultPos) as HTMLElement;
    if (container && resultNode) {
      const scroller = zenscroll.createScroller(containerEl, duration, offset);
      if (center) {
        scroller.center(resultNode, duration, offset, onDone);
      } else {
        scroller.intoView(resultNode, duration, onDone);
      }
    }
  }
}

export function scrollToPos(view: EditorView, pos: number, duration?: number, offset?: number, onDone?: VoidFunction) {
  const node = view.nodeDOM(pos);
  if (node instanceof HTMLElement) {
    const editingRoot = editingRootNode(view.state.selection)!;
    const container = view.nodeDOM(editingRoot.pos) as HTMLElement;
    const scroller = zenscroll.createScroller(container, duration, offset);
    if (duration) {
      scroller.to(node, duration, onDone);
    } else {
      scroller.to(node, 0, onDone);
    }
  }
}
