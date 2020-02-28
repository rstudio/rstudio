/*
 * link-popup.ts
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

import { DecorationSet, Decoration } from "prosemirror-view";
import { Plugin, PluginKey, EditorState, Transaction } from "prosemirror-state";

import { getMarkRange, getMarkAttrs } from "../../api/mark";
import { LinkProps } from "../../api/ui";

// https://prosemirror.net/examples/tooltip/. see:
// https://glitch.com/edit/#!/lackadaisical-coffee-streetcar

// /https://prosemirror.net/examples/lint/ (just use from right before the link then
// use css to push it down and outside of the doc flow). take advantage of the fact
// that absolutely positioned elements are positioned where they sit in the document
// if explicit top/bottom/left/right/etc. properties aren't set. See:
// https://glitch.com/edit/#!/octagonal-brazen-utahraptor?path=index.html:1:0
// just do this and give it a margin-top!


const key = new PluginKey<DecorationSet>('link-popup');

export class LinkPopupPlugin extends Plugin<DecorationSet> {
 
  constructor() {
    super({
      key,
      state: {
        init: (_config: { [key: string]: any }, instance: EditorState) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          
          // if the selection is contained within a link then show the popup
          const schema = tr.doc.type.schema;
          const selection = tr.selection;
          const range = getMarkRange(selection.$head, schema.marks.link);
          if (range) {
            const attrs = getMarkAttrs(tr.doc, tr.selection, schema.marks.link) as LinkProps;

            // TODO: needs to use range.to if the link is on the right side of the screen
            
            // TODO: alternatively we could use an implementation more like the tooltip
            // (plugin that has access to the view)

            return DecorationSet.create(tr.doc, [Decoration.widget(range.from, linkPopup(attrs))]);
          } else {
            return DecorationSet.empty;
          }
        },
      },
      props: {
        decorations: (state: EditorState) => {
          return key.getState(state);
        },
      },
    });
  }
}

function linkPopup(attrs: LinkProps) {
  const popup = window.document.createElement("div");
  popup.style.position = "absolute";
  popup.style.marginTop = "1.2em";
  popup.style.backgroundColor = "pink";
  popup.style.display = "inline-block";
  popup.innerText = attrs.href;
  return popup;
}

