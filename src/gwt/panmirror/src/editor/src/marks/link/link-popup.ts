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
import { LinkProps, EditorUI } from "../../api/ui";
import { editingRootNode } from "../../api/node";
import { CommandFn } from "../../api/command";
import { kRestoreLocationTransaction } from "../../api/transaction";
import { createInlineTextPopup, createLink, createImageButton } from "../../api/widgets";

const kMaxLinkWidth = 400;

const key = new PluginKey<DecorationSet>('link-popup');

export class LinkPopupPlugin extends Plugin<DecorationSet> {
 
  constructor(ui: EditorUI, linkCmd: CommandFn, removeLinkCmd: CommandFn) {

    let editorView: EditorView;

    function linkPopup(attrs: LinkProps, style?: { [key: string]: string }) {
      
      // create popup. offset left -1ch b/c we use range.from + 1 to position the popup
      // (this is so that links that start a line don't have their ragne derived from
      // the previous line)
      const popup = createInlineTextPopup(["pm-popup-link"], { 'margin-left': '-1ch', ...style } );

      // create link
      const link = createLink(attrs.href, kMaxLinkWidth);
      link.onclick = () => {
        ui.display.openURL(attrs.href);
        return false;
      };
      popup.append(link);

      // create image butttons
      const editLink = createImageButton(["pm-image-button-edit-link"]);
      editLink.onclick = () => {
        linkCmd(editorView.state, editorView.dispatch, editorView);
      };
      popup.append(editLink);

      const removeLink = createImageButton(["pm-image-button-remove-link"]);
      removeLink.onclick = () => {
        removeLinkCmd(editorView.state, editorView.dispatch, editorView);
      };
      popup.append(removeLink);

      return popup;
    }

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
            const attrs = getMarkAttrs(tr.doc, range, schema.marks.link) as LinkProps;
           
            // get the (window) DOM coordinates for the start of the mark. we use range.from + 1 so 
            // that links that are at the beginning of a line don't have their position set
            // to the previous line
            const linkCoords = editorView.coordsAtPos(range.from + 1);

            // get the (window) DOM coordinates for the current editing root note (body or notes)
            const editingNode = editingRootNode(selection);
            const editingEl = editorView.domAtPos(editingNode!.pos + 1).node as HTMLElement;
            const editingBox = editingEl.getBoundingClientRect();

            // we need to compute whether the popup will be visible (horizontally), do
            // this by testing whether we have room for the max link width + controls/padding
            const kPopupChromeWidth = 70;
            const positionRight = (linkCoords.left + kMaxLinkWidth + kPopupChromeWidth) > editingBox.right;
            let popup: HTMLElement;
            if (positionRight) {
              const linkRightCoords = editorView.coordsAtPos(range.to);
              const linkRightPos = editingBox.right - linkRightCoords.right;
              popup = linkPopup(attrs, { right: linkRightPos + "px"});
            } else {
              popup = linkPopup(attrs);
            }

            // return decorations
            return DecorationSet.create(tr.doc, [Decoration.widget(range.from + 1, popup)]);
           
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



