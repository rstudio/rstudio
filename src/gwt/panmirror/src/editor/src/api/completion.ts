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

import { EditorState } from "prosemirror-state";
import { Node as ProsemirrorNode  } from "prosemirror-model";


// TODO: some sort of invalidation token for requests (b/c they are async)

export interface CompletionHandler<T = any> {

  // can this handler produce completions for theh given context? (lower level handler)
  // if so return the position from which replacements will occur
  canCompleteAt(state: EditorState): number | null;

  // return a set of completions for the given context
  completions(state: EditorState, limit: number): Promise<T[]>;
  
  // provide a react compontent type for viewing the item
  completionView: React.FC<T> | React.ComponentClass<T>;

  // provide a completion replacement as a string or node
  replacement(completion: T) : string | ProsemirrorNode;

}



