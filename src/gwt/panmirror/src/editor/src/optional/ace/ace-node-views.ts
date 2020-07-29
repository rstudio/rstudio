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

    /*
    // see if the click is between 2 contiguously located node views
    for (const nodeView of this.nodeViews) {

      // alias stuff we need for the computation
      const { pos, node } = nodeView.getPosAndNode();
      const dom = nodeView.dom;



    }

    console.log(this.nodeViews.length + ' node views');
    */

    return false;
  }

}