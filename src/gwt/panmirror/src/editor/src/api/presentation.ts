/*
 * presentation.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import { EditorState } from 'prosemirror-state';

import { findTopLevelBodyNodes } from './node';
import { titleFromState } from './yaml';

export interface PresentationEditorLocation {
  items: PresentationEditorLocationItem[];
}

export const kPresentationEditorLocationTitle = "title";
export const kPresentationEditorLocationHeading = "heading";
export const kPresentationEditorLocationHr = "hr";
export const kPresentationEditorLocationCursor = "cursor";

export interface PresentationEditorLocationItem {
  type: string;
  level: number;
}

export function getPresentationEditorLocation(state: EditorState) : PresentationEditorLocation {
  
  // not the cursor position
  const cursorPos = state.selection.from;

  // build list of items
  const items: PresentationEditorLocationItem[] = [];

  // start with title if we have one. note that pandoc will make the title slide
  // first no matter where it appears in the document, so we do the same here
  const title = titleFromState(state);
  if (title) {
    items.push({ type: kPresentationEditorLocationTitle, level: 0} );
  }

  // get top level headings and horizontal rules
  const schema = state.schema;
  const bodyNodes = findTopLevelBodyNodes(state.doc, node => {
    return [
      schema.nodes.heading,
      schema.nodes.horizontal_rule,
    ].includes(node.type);
  });
  let foundCursor = false;
  for (const nodeWithPos of bodyNodes) {
    // if node is past the selection then add the cursor token
    if (!foundCursor && (nodeWithPos.pos > cursorPos)) {
      foundCursor = true;
      items.push({
        type: kPresentationEditorLocationCursor,
        level: 0
      });
    }
    // add the node with the requisite type
    const node = nodeWithPos.node;
    if (node.type === schema.nodes.heading) {
      items.push({
        type: kPresentationEditorLocationHeading,
        level: node.attrs.level || 0
      });
    } else {
      items.push({
        type: kPresentationEditorLocationHr,
        level: 0
      });
    }
  }

  // return the tokens
  return { items };
}

export function navigateToPresentationEditorLocation(_location: PresentationEditorLocation) {
  // TODO: implement navigation
}
