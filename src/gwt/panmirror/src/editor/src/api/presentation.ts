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
  // extra field we use internally for navigation
  pos: number;
}

export function getPresentationEditorLocation(state: EditorState) : PresentationEditorLocation {
  
  // not the cursor position
  const cursorPos = state.selection.from;

  // build list of items
  const items: PresentationEditorLocationItem[] = [];

  // get top level headings and horizontal rules
  const schema = state.schema;
  const bodyNodes = findTopLevelBodyNodes(state.doc, node => {
    return [
      schema.nodes.heading,
      schema.nodes.horizontal_rule,
    ].includes(node.type);
  });

  // bail if empty
  if (bodyNodes.length === 0) {
    return { items };
  }

  // start with title if we have one. note that pandoc will make the title slide
  // first no matter where it appears in the document, so we do the same here
  const title = titleFromState(state);
  if (title) {
    items.push({ 
      type: kPresentationEditorLocationTitle, 
      level: 0,
      pos: bodyNodes[0].pos
    });
  }

  // get top level headings and horizontal rules
  let foundCursor = false;
  for (const nodeWithPos of bodyNodes) {
    // if node is past the selection then add the cursor token
    if (!foundCursor && (nodeWithPos.pos > cursorPos)) {
      foundCursor = true;
      items.push({
        type: kPresentationEditorLocationCursor,
        level: 0,
        pos: nodeWithPos.pos
      });
    }
    // add the node with the requisite type
    const node = nodeWithPos.node;
    if (node.type === schema.nodes.heading) {
      items.push({
        type: kPresentationEditorLocationHeading,
        level: node.attrs.level || 0,
        pos: nodeWithPos.pos
      });
    } else {
      items.push({
        type: kPresentationEditorLocationHr,
        level: 0,
        pos: nodeWithPos.pos
      });
    }
  }

  // return the tokens
  return { items };
}

export function positionForPresentationEditorLocation(
  state: EditorState, 
  location: PresentationEditorLocation
) : number {
  // get the positions of the editor's current state (filter out cursor)
  const editorItems = getPresentationEditorLocation(state).items
    .filter(item => item.type !== kPresentationEditorLocationCursor);
  
  // get the index of the cursor passed in the location
  const cursorIdx = location.items.findIndex(
    item => item.type === kPresentationEditorLocationCursor)
  ;

  // go one slide before the cursor
  if (cursorIdx > 0) {
    const locationItem = editorItems[cursorIdx - 1];
    return locationItem.pos;
  }

  // default if we can't find a location
  return -1;
}
