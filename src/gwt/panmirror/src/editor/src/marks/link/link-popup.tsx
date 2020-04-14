/*
 * LinkPopup.tsx
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

import * as React from 'react';

import ClipboardJS from 'clipboard';

import { getMarkRange, getMarkAttrs } from '../../api/mark';
import { LinkProps, EditorUI } from '../../api/ui';
import { CommandFn } from '../../api/command';
import { kRestoreLocationTransaction } from '../../api/transaction';

import { navigateToId, navigateToHeading } from '../../api/navigation';
import { selectionIsImageNode } from '../../api/selection';

import { showTooltip } from '../../api/widgets/tooltip';

import { reactRenderForEditorView, WidgetProps } from '../../api/widgets/react';
import { Panel } from '../../api/widgets/panel';
import { LinkButton, ImageButton } from '../../api/widgets/button';
import { textRangePopupDecorationPosition } from '../../api/widgets/decoration';
import { Popup } from '../../api/widgets/popup';

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
        apply: (tr: Transaction, old: DecorationSet, _oldState: EditorState, newState: EditorState) => {
          // if this a restore location then return empty
          if (tr.getMeta(kRestoreLocationTransaction)) {
            return DecorationSet.empty;
          }

          // if the selection is contained within a link then show the popup
          const schema = newState.doc.type.schema;
          const selection = newState.selection;

          // don't show the link popup if the selection is an image node (as it has it's own popup)
          if (selectionIsImageNode(schema, selection)) {
            return DecorationSet.empty;
          }

          const range = getMarkRange(selection.$from, schema.marks.link);
          if (range) {
            // don't show the link popup if it's positioned at the far left of the link
            // (awkward when cursor is just left of an image)
            if (selection.empty && range.from === selection.from) {
              return DecorationSet.empty;
            }

            // link attrs
            const attrs = getMarkAttrs(newState.doc, range, schema.marks.link) as LinkProps;

            // compute position (we need this both for setting the styles on the LinkPopup
            // as well as for setting the Decorator pos)
            const kPopupChromeWidth = 70;
            const kMaxLinkWidth = 300;
            const maxWidth = kMaxLinkWidth + kPopupChromeWidth;
            const decorationPosition = textRangePopupDecorationPosition(editorView, range, maxWidth);

            // compute unique key (will allow us to only recreate the popup when necessary)
            const linkText = attrs.heading ? attrs.heading : attrs.href;
            const specKey = `link_popup_decoration_pos:${decorationPosition.pos}link:${linkText}`;

            // if the old popup already has a decoration for this position then just use it
            if (old.find(undefined, undefined, spec => spec.key === specKey).length) {
              return old.map(tr.mapping, tr.doc);
            }

            // create link popup component
            const popup = (
              <LinkPopup
                link={attrs}
                linkCmd={linkCmd}
                removeLinkCmd={removeLinkCmd}
                view={editorView}
                ui={ui}
                style={decorationPosition.style}
              />
            );

            // create decorator and render popup into it
            const decoration = window.document.createElement('div');
            reactRenderForEditorView(popup, decoration, editorView);

            // return decorations
            return DecorationSet.create(tr.doc, [Decoration.widget(decorationPosition.pos, decoration, { key: specKey })]);
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

interface LinkPopupProps extends WidgetProps {
  link: LinkProps;
  view: EditorView;
  ui: EditorUI;
  linkCmd: CommandFn;
  removeLinkCmd: CommandFn;
}

const LinkPopup: React.FC<LinkPopupProps> = props => {
  // link
  const linkText = props.link.heading ? props.link.heading : props.link.href;
  const onLinkClicked = () => {
    props.view.focus();
    if (props.link.heading) {
      navigateToHeading(props.view, props.link.heading);
    } else if (props.link.href.startsWith('#')) {
      navigateToId(props.view, props.link.href.substr(1));
    } else {
      props.ui.display.openURL(props.link.href);
    }
  };

  // copy
  const showCopyButton = !props.link.heading && ClipboardJS.isSupported();
  let clipboard: ClipboardJS;
  const setCopyButton = (button: HTMLButtonElement | null) => {
    if (button) {
      clipboard = new ClipboardJS(button, {
        text: () => linkText,
      });
      clipboard.on('success', () => {
        showTooltip(button, props.ui.context.translateText('Copied to Clipboard'), 's');
      });
    } else {
      if (clipboard) {
        clipboard.destroy();
      }
    }
  };

  // remove
  const onRemoveClicked = () => {
    // in rstudio (w/ webkit) removing the link during the click results
    // in a page-navigation! defer to next event cycle to avoid this
    setTimeout(() => {
      props.removeLinkCmd(props.view.state, props.view.dispatch, props.view);
      props.view.focus();
    }, 0);
  };

  // edit
  const onEditClicked = () => {
    props.linkCmd(props.view.state, props.view.dispatch, props.view);
  };

  return (
    <Popup classes={['pm-popup-link']} style={props.style}>
      <Panel>
        <LinkButton text={linkText} onClick={onLinkClicked}></LinkButton>
        {showCopyButton ? (
          <ImageButton
            classes={['pm-image-button-copy-link']}
            title={props.ui.context.translateText('Copy Link to Clipboard')}
            ref={setCopyButton}
          />
        ) : null}
        <ImageButton
          classes={['pm-image-button-remove-link']}
          title={props.ui.context.translateText('Remove Link')}
          onClick={onRemoveClicked}
        />
        <ImageButton
          classes={['pm-image-button-edit-properties']}
          title={props.ui.context.translateText('Edit Attributes')}
          onClick={onEditClicked}
        />
      </Panel>
    </Popup>
  );
};
