/*
 * link-command.ts
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

import { MarkType } from 'prosemirror-model';
import { LinkEditorFn, LinkProps } from '../../api/ui';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { markIsActive, getMarkAttrs, getSelectionMarkRange, getMarkRange } from '../../api/mark';

import { linkTargets, LinkCapabilities, LinkType } from '../../api/link';

export function linkCommand(markType: MarkType, onEditLink: LinkEditorFn, capabilities: LinkCapabilities) {
  return (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
    // if the current node doesn't allow this mark return false
    if (!state.selection.$from.node().type.allowsMarkType(markType)) {
      return false;
    }

    async function asyncEditLink() {
      if (dispatch) {
        // collect link targets
        const targets = await linkTargets(state.doc);

        // get the range of the mark
        const range = getSelectionMarkRange(state.selection, markType);

        // get link attributes if we have them
        let link: { [key: string]: any } = {};
        link.text = state.doc.textBetween(range.from, range.to);
        if (markIsActive(state, markType)) {
          link = {
            ...link,
            ...getMarkAttrs(state.doc, state.selection, markType),
          };
        }

        // determine type
        if (link.heading) {
          link.type = LinkType.Heading;
        } else if (link.href && link.href.startsWith('#')) {
          link.type = LinkType.ID;
        } else {
          link.type = LinkType.URL;
        }

        // show edit ui
        const result = await onEditLink({ ...link } as LinkProps, targets, capabilities);
        if (result) {
          const tr = state.tr;
          tr.removeMark(range.from, range.to, markType);
          if (result.action === 'edit') {
            // create the link attributes
            const attrs = {
              ...result.link,
              heading: result.link.type === LinkType.Heading ? result.link.href : undefined,
            };

            // create the mark
            const mark = markType.create(attrs);

            // if the content changed then replace the range, otherwise
            if (link.text !== result.link.text) {
              const node = markType.schema.text(result.link.text, [mark]);
              // if we are editing an existing link then replace it, otherwise replace the selection
              if (link.href) {
                tr.replaceRangeWith(range.from, range.to, node);
              } else {
                tr.replaceSelectionWith(node, false);
              }
            } else {
              tr.addMark(range.from, range.to, mark);
            }
          }
          dispatch(tr);
        }
        if (view) {
          view.focus();
        }
      }
    }
    asyncEditLink();

    return true;
  };
}

export function removeLinkCommand(markType: MarkType) {
  return (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
    
    const range = getMarkRange(state.selection.$head, markType);
    if (!range) {
      return false;
    }

    if (dispatch) {
      const tr = state.tr;
      tr.removeMark(range.from, range.to, markType);
      dispatch(tr);
    }

    return true;
  };
}


