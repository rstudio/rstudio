/*
 * math.ts
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


import { PromiseQueue } from "./promise";
import { EditorUI } from "./ui";

export interface EditorMath {
  typeset: (el: HTMLElement, math: string) => Promise<boolean>;
}

export function editorMath(ui: EditorUI): EditorMath {

  // queue so we only do one typeset at a time
  const typesetQueue = new PromiseQueue<boolean>();

  // return a promise that will typeset this node's math (including retrying as long as is
  // required if the element is not yet connected to the DOM)
  return {

    typeset: (el: HTMLElement, math: string): Promise<boolean> => {

      // place the typeset request on the global queue
      return typesetQueue.enqueue(() => {
        return new Promise(resolve => {
          // regular typeset if we are already connected
          if (el.isConnected) {
            ui.math.typeset!(el, math).then(resolve);
          } else {
            // otherwise wait 100ms then retry
            const timerId = setInterval(() => {
              if (el.isConnected) {
                clearInterval(timerId);
                ui.math.typeset!(el, math).then(resolve);
              }
            }, 100);
          }
        });
      });

    }
  };
}
