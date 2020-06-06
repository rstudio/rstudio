/*
 * completion.ts
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

import { Selection, EditorState } from "prosemirror-state";
import { Node as ProsemirrorNode, Schema  } from "prosemirror-model";
import { EditorView } from "prosemirror-view";

import { canInsertNode } from "./node";


export const kCompletionDefaultItemHeight = 22;
export const kCompletionDefaultMaxVisible = 10;
export const kCompletionDefaultWidth = 180;

export interface CompletionResult<T = any> {
  pos: number;
  completions: (state: EditorState) => Promise<T[]>;
}

export interface CompletionHandler<T = any> {

  // return a set of completions for the given context. text is the text before
  // before the cursor in the current node (but no more than 500 characters)
  completions(text: string, doc: ProsemirrorNode, selection: Selection): CompletionResult | null;

  // provide a completion replacement as a string or node (can be passed null if the popup was dismissed)
  replacement?(schema: Schema, completion: T | null)  : string | ProsemirrorNode | null;

  // lower level replacement handler (can be passed null if the popup was dismissed)
  replace?(view: EditorView, pos: number, completion: T | null) : void;

  // completion view
  view: {
    // react compontent type for viewing the item
    component: React.FC<T> | React.ComponentClass<T>;

    key: (completion: T) => any;

    // width of completion popup (defaults to 180)
    width?: number;

    // height for completion items (defaults to 22px)
    itemHeight?: number;
 
    // maximum number of visible items (defaults to 10)
    maxVisible?: number;
  };
}


export function selectionAllowsCompletions(selection: Selection) {

  const schema = selection.$head.parent.type.schema;

  // non empty selections don't have completions
  if (!selection.empty) {
    return false;
  }

  // must be able to insert text
  if (!canInsertNode(selection, schema.nodes.text)) {
    return false;
  }

  // must not be in a code mark 
  if (!!schema.marks.code.isInSet(selection.$from.marks())) {
    return false;
  }

  // must not be in a code node
  if (selection.$head.parent.type.spec.code) {
    return false;
  }

  return true;

}
