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

import { DecorationSet, Decoration, EditorView } from 'prosemirror-view';
import { Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';

import ClipboardJS from 'clipboard';

import { getMarkRange, getMarkAttrs } from '../../api/mark';
import { LinkProps, EditorUI } from '../../api/ui';
import { editingRootNode } from '../../api/node';
import { CommandFn } from '../../api/command';
import { kRestoreLocationTransaction } from '../../api/transaction';
import {
  createLinkButton,
  createImageButton,
  createHorizontalPanel,
  addHorizontalPanelCell,
  showTooltip,
  createPopup,
} from '../../api/widgets';
import { navigateToId, navigateToHeading } from '../../api/navigation';

const kMaxLinkWidth = 300;

const key = new PluginKey<DecorationSet>('link-popup');

export class LinkPopupPlugin extends Plugin<DecorationSet> {
  constructor(ui: EditorUI, linkCmd: CommandFn, removeLinkCmd: CommandFn) {
    let editorView: EditorView;

    function linkPopup(attrs: LinkProps, style?: { [key: string]: string }) {
      // we may create a ClipboardJS instance. if we do then clean it up
      // when the popup id destroyed
      let clipboard: ClipboardJS;
      const cleanup = () => {
        if (clipboard) {
          clipboard.destroy();
        }
      };

      // create popup. offset left -1ch b/c we use range.from + 1 to position the popup
      // (this is so that links that start a line don't have their ragne derived from
      // the previous line)
      const popup = createPopup(editorView, ['pm-popup-link'], cleanup, { 
        'margin-left': '-1ch', 
        'margin-top': '1.5em',
        ...style 
      });

      const panel = createHorizontalPanel();
      popup.append(panel);

      // create link
      const text = attrs.heading ? attrs.heading! : attrs.href;
      const link = createLinkButton(text, attrs.title, kMaxLinkWidth);
      link.onclick = () => {
        editorView.focus();
        if (attrs.heading) {
          navigateToHeading(editorView, attrs.heading);
        } else if (attrs.href.startsWith('#')) {
          navigateToId(editorView, attrs.href.substr(1));
        } else {
          ui.display.openURL(attrs.href);
        }
        return false;
      };
      addHorizontalPanelCell(panel, link);

      // copy link
      if (!attrs.heading && ClipboardJS.isSupported()) {
        const copyLink = createImageButton(
          ['pm-image-button-copy-link'],
          ui.context.translateText('Copy Link to Clipboard'),
        );
        clipboard = new ClipboardJS(copyLink, {
          text: () => text,
        });
        clipboard.on('success', () => {
          showTooltip(copyLink, ui.context.translateText('Copied to Clipboard'), 's');
        });
        addHorizontalPanelCell(panel, copyLink);
      }

      // edit link
      const editLink = createImageButton(['pm-image-button-edit-link'], ui.context.translateText('Edit Link'));
      editLink.onclick = () => {
        linkCmd(editorView.state, editorView.dispatch, editorView);
      };
      addHorizontalPanelCell(panel, editLink);

      // remove link
      const removeLink = createImageButton(['pm-image-button-remove-link'], ui.context.translateText('Remove Link'));
      removeLink.onclick = () => {
        // in rstudio (w/ webkit) removing the link during the click results
        // in a page-navigation! defer to next event cycle to avoid this
        setTimeout(() => {
          removeLinkCmd(editorView.state, editorView.dispatch, editorView);
          editorView.focus();
        }, 0);
      };
      addHorizontalPanelCell(panel, removeLink);

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
          const range = getMarkRange(selection.$from, schema.marks.link);
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
            const positionRight = linkCoords.left + kMaxLinkWidth + kPopupChromeWidth > editingBox.right;
            let popup: HTMLElement;
            if (positionRight) {
              const linkRightCoords = editorView.coordsAtPos(range.to);
              const linkRightPos = editingBox.right - linkRightCoords.right;
              popup = linkPopup(attrs, { right: linkRightPos + 'px' });
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
