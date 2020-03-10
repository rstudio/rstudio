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
import { CommandFn } from '../../api/command';
import { kRestoreLocationTransaction } from '../../api/transaction';
import {
  createLinkButton,
  createImageButton,
  createHorizontalPanel,
  addHorizontalPanelCell,
  showTooltip,
  createTextRangePopup,
} from '../../api/widgets';
import { navigateToId, navigateToHeading } from '../../api/navigation';

const kMaxLinkWidth = 300;

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
        init: (_config: { [key: string]: any }) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, _old: DecorationSet, _oldState: EditorState, newState: EditorState) => {
          // if this a restore location then return empty
          if (tr.getMeta(kRestoreLocationTransaction)) {
            return DecorationSet.empty;
          }

          // if the selection is contained within a link then show the popup
          const schema = newState.doc.type.schema;
          const selection = newState.selection;
          const range = getMarkRange(selection.$from, schema.marks.link);
          if (range) {
            // get link attrs
            const attrs = getMarkAttrs(editorView.state.doc, range, schema.marks.link) as LinkProps;

            // we may create a ClipboardJS instance. if we do then clean it up
            // when the popup id destroyed
            let clipboard: ClipboardJS;
            const cleanup = () => {
              if (clipboard) {
                clipboard.destroy();
              }
            };

            // create popup. offset left -1ch b/c we use range.from + 1 to position the popup
            // (this is so that ranges that start a line don't have their ragne derived from
            // the previous line)
            const kPopupChromeWidth = 70;
            const maxWidth = kMaxLinkWidth + kPopupChromeWidth;
            const textRangePopup = createTextRangePopup(
              editorView, 
              range, 
              ['pm-popup-link'], 
              maxWidth, 
              cleanup
            );
        
            // create panel that will host the ui and add it to the popup
            const panel = createHorizontalPanel();
            textRangePopup.popup.append(panel);
            const addToPanel = (widget: HTMLElement) => {
              addHorizontalPanelCell(panel, widget);
            };

            // link
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
            addToPanel(link);

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
              addToPanel(copyLink);
            }

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
            addToPanel(removeLink);

            // edit link
            const editLink = createImageButton(['pm-image-button-edit-properties'], ui.context.translateText('Edit Link'));
            editLink.onclick = () => {
              linkCmd(editorView.state, editorView.dispatch, editorView);
            };
            addToPanel(editLink);           

            // return decorations
            return DecorationSet.create(tr.doc, [Decoration.widget(textRangePopup.pos, textRangePopup.popup)]);
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
