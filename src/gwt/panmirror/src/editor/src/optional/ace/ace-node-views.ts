/*
 * ace-node-views.ts
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

import { GapCursor } from "prosemirror-gapcursor";
import { AceNodeView } from "./ace";
import { EditorView } from "prosemirror-view";

/**
 * Track all Ace node view instances to implement additional behavior 
 * (e.g. gap cursor for clicks between editor instances)
 */

export class AceNodeViews {

  private nodeViews: AceNodeView[];

  constructor() {
    this.nodeViews = [];
  }
  public add(nodeView: AceNodeView) {
    this.nodeViews.push(nodeView);
  }

  public remove(nodeView: AceNodeView) {
    const index = this.nodeViews.indexOf(nodeView);
    if (index >= 0) {
      this.nodeViews.splice(index, 1);
    }
  }

  public handleClick(view: EditorView, event: Event): boolean {

    // alias to mouseEvent
    const mouseEvent = event as MouseEvent;

    // see if the click is between 2 contiguously located node views
    for (const nodeView of this.nodeViews) {

      // gap cursor we might detect
      let gapCursor: GapCursor | null = null;

      // get the position 
      const pos = nodeView.getPos();
      const $pos = view.state.doc.resolve(pos);

      // if the previous node is code, see if the click is between the 2 nodes
      if ($pos.nodeBefore && $pos.nodeBefore.type.spec.code) {

        // get our bounding rect
        const dom = nodeView.dom;
        const nodeViewRect = dom.getBoundingClientRect();

        // get the previous node's bounding rect
        const prevNodePos = pos - $pos.nodeBefore!.nodeSize;
        const prevNodeView = this.nodeViews.find(nv => nv.getPos() === prevNodePos);
        if (prevNodeView) {
          const prevNodeRect = prevNodeView.dom.getBoundingClientRect();

          // check for a click between the two nodes
          const mouseY = mouseEvent.clientY;
          if (mouseY > (prevNodeRect.top + prevNodeRect.height) && mouseY < (nodeViewRect.top)) {
            gapCursor = new GapCursor($pos, $pos);
          }
        }

        // if there is no previous node and the click is above us then gap cursor above
      } else if (!$pos.nodeBefore && $pos.depth === 1 && (mouseEvent.clientY < nodeView.dom.getBoundingClientRect().top)) {
        gapCursor = new GapCursor($pos, $pos);
      }

      // return gapCursor if we found one
      if (gapCursor) {
        const tr = view.state.tr;

        // notify the node views that we are setting a gap cursor
        this.nodeViews.forEach(ndView => ndView.setGapCursorPending(true));

        // ensure the view is focused 
        view.focus();

        // set the selection
        tr.setSelection(gapCursor);
        view.dispatch(tr);

        // notify the node views that we are done setting the gap cursor
        this.nodeViews.forEach(ndView => ndView.setGapCursorPending(false));

        // prevent default event handling
        event.preventDefault();
        event.stopImmediatePropagation();
        return true;
      }
    }

    return false;
  }

}