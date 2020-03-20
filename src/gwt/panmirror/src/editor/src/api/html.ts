/*
 * html.ts
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

import { Node as ProsemirrorNode, Schema, DOMSerializer, Fragment } from 'prosemirror-model';

export function nodeToHTML(schema: Schema, node: ProsemirrorNode) {
  return generateHTML(() => DOMSerializer.fromSchema(schema).serializeNode(node));
}

export function fragmentToHTML(schema: Schema, fragment: Fragment) {
  return generateHTML(() => DOMSerializer.fromSchema(schema).serializeFragment(fragment));
}

function generateHTML(generator: () => Node | DocumentFragment) {
  const div = document.createElement('div');
  const output = generator();
  div.appendChild(output);
  return div.innerHTML;
}