/*
 * slice.ts
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

import { Slice, Node as ProsemirrorNode } from 'prosemirror-model';

export function sliceContentLength(slice: Slice) {
  let length = 0;
  for (let i = 0; i < slice.content.childCount; i++) {
    length += slice.content.child(i).textContent.length;
  }
  return length;
}

export function sliceHasNode(slice: Slice, predicate: (node: ProsemirrorNode) => boolean) {
  let hasNode = false;
  slice.content.descendants(node => {
    if (predicate(node)) {
      hasNode = true;
      return false;
    }
  });
  return hasNode;
}
