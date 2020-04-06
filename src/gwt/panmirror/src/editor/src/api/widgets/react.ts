/*
 * react.ts
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


import * as React from 'react';
import * as ReactDOM from 'react-dom';

import { EditorView } from 'prosemirror-view';

export interface WidgetProps {
  className?: string;
}

export function reactNodeDecorator(view: EditorView, element: React.ReactElement) {

  // create decorator element that will be returned to prosemirror
  const decorator = window.document.createElement("div");

  // render the react element into the decorator div
  ReactDOM.render(element, decorator);

  // track view dom mutations to determine when ProseMirror has destroyed the element
  // (our cue to unmount/cleanup the react component)
  const observer = new MutationObserver(mutationsList => {
    mutationsList.forEach(mutation => {
      mutation.removedNodes.forEach(node => {
        if (node === decorator) {
          observer.disconnect();
          ReactDOM.unmountComponentAtNode(decorator);
        }
      });
    });
  });
  observer.observe(view.dom, { attributes: false, childList: true, subtree: true });

  // return the decorator div
  return decorator;
}

