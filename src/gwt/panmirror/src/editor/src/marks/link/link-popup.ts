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

import { DecorationSet, Decoration, EditorView } from "prosemirror-view";
import { Plugin, PluginKey, EditorState, Transaction } from "prosemirror-state";

import { getMarkRange, getMarkAttrs } from "../../api/mark";
import { LinkProps, EditorUI, PopupLinkResult } from "../../api/ui";
import { editingRootNode } from "../../api/node";
import { CommandFn } from "../../api/command";
import { kRestoreLocationTransaction } from "../../api/transaction";

// popup positioning based on:
//   https://prosemirror.net/examples/lint/
//   https://glitch.com/edit/#!/octagonal-brazen-utahraptor
// take advantage of the fact that absolutely positioned elements are positioned where 
// they sit in the document if explicit top/bottom/left/right/etc. properties aren't set.

const key = new PluginKey<DecorationSet>('link-popup');

export class LinkPopupPlugin extends Plugin<DecorationSet> {
 
  constructor(ui: EditorUI, linkCmd: CommandFn, removeLinkCmd: CommandFn) {

    let editorView: EditorView;

    super({
      key,
      view(view: EditorView) {
        editorView = view;
        return {};
      },
      state: {
        init: (_config: { [key: string]: any }, instance: EditorState) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, oldState: EditorState, newState: EditorState) => {
          
          // if there is no link popup ui then just return empty
          if (!ui.dialogs.popupLink) {
            return DecorationSet.empty;
          }

          // if this a restore location then return empty
          if (tr.getMeta(kRestoreLocationTransaction)) {
            return DecorationSet.empty;
          }

          // if the selection is contained within a link then show the popup
          const schema = tr.doc.type.schema;
          const selection = tr.selection;
          const range = getMarkRange(selection.$head, schema.marks.link);
          if (range) {

            // get link attributes
            const attrs = getMarkAttrs(tr.doc, tr.selection, schema.marks.link) as LinkProps;
           
            // get the (window) DOM coordinates for the start of the mark
            const linkCoords = editorView.coordsAtPos(range.from);

            // get the (window) DOM coordinates for the current editing root note (body or notes)
            const editingNode = editingRootNode(selection);
            const editingEl = editorView.domAtPos(editingNode!.pos + 1).node as HTMLElement;
            const editingBox = editingEl.getBoundingClientRect();

            // we need to compute whether the popup will be visible (horizontally), do
            // this by testing whether this room for 400px
            const kMaxPopupWidth = 400;
            const positionRight = (linkCoords.left + kMaxPopupWidth) > editingBox.right;
            let popup: HTMLElement;
            if (positionRight) {
              const linkRightCoords = editorView.coordsAtPos(range.to);
              const linkRightPos = editingBox.right - linkRightCoords.right;
              popup = linkPopup({ right: linkRightPos + "px"});
            } else {
              popup = linkPopup();
            }

            const  showPopupAsync = async () => {
              const result = await ui.dialogs.popupLink!(popup, attrs.href, kMaxPopupWidth);
              switch(result) {
                case PopupLinkResult.Edit:
                  linkCmd(editorView.state, editorView.dispatch, editorView);
                  break;
                case PopupLinkResult.Remove:
                  removeLinkCmd(editorView.state, editorView.dispatch, editorView);
                  break;
              }
            };
            showPopupAsync();

            // return decorations
            return DecorationSet.create(tr.doc, [Decoration.widget(range.from, popup)]);
           
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

function linkPopup(style?: { [key: string]: string }) {
  const popup = window.document.createElement("div");
  popup.classList.add("pm-inline-text-popup");
  popup.style.position = "absolute";
  popup.style.display = "inline-block";
  if (style) {
    Object.keys(style).forEach(name => {
      popup.style.setProperty(name, style[name]);
    });
  }
  return popup;
}

