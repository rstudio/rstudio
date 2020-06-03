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

import { Selection } from "prosemirror-state";
import { Node as ProsemirrorNode  } from "prosemirror-model";


export interface CompletionResult<T = any> {
  pos: number;
  items: T[] | Promise<T[]>;
}

export interface CompletionHandler<T = any> {

  // return a set of completions for the given context
  completions(selection: Selection): CompletionResult | null;
  
  // provide a completion replacement as a string or node
  replacement(completion: T) : string | ProsemirrorNode;

  // provide a react compontent type for viewing the item
  itemView: React.FC<T> | React.ComponentClass<T>;

  // height for completion items (defaults to 20px)
  itemHeight?: number;
 
  // maximum number of visible items (defaults to 10)
  maxVisible?: number;

  // width of completion popup (defaults to 180)
  width?: number;
}



