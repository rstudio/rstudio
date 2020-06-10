/*
 * omni_insert.tsx
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

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorState, Selection } from 'prosemirror-state';
import { EditorView, DecorationSet } from 'prosemirror-view';

import React from 'react';

import { firstBy } from 'thenby';

import { OmniInserter, omniInsertGroupCompare, omniInsertPriorityCompare } from '../../api/omni_insert';
import { CompletionHandler, CompletionResult } from '../../api/completion';

import './omni_insert-completion.css';
import { EditorUI } from '../../api/ui';
import { placeholderDecoration } from '../../api/placeholder';
import { kAddToHistoryTransaction } from '../../api/transaction';
import { setTextSelection } from 'prosemirror-utils';

export function omniInsertCompletionHandler(
  omniInserters: OmniInserter[],
  ui: EditorUI,
): CompletionHandler<OmniInserter> {
  return {
    completions: omniInsertCompletions(omniInserters, ui),

    replace(view: EditorView, pos: number, completion: OmniInserter | null) {
      // helper to remove command text
      const removeCommandText = () => {
        const tr = view.state.tr;
        tr.deleteRange(pos, view.state.selection.head);
        tr.setMeta(kAddToHistoryTransaction, false);
        view.dispatch(tr);
      };

      // execute command if provided
      if (completion) {
        // remove existing text
        removeCommandText();

        // execute the command
        completion.command(view.state, view.dispatch, view);

        // perform any requested selection offset
        if (completion.selectionOffset) {
          const tr = view.state.tr;
          setTextSelection(tr.selection.from + completion.selectionOffset)(tr);
          tr.setMeta(kAddToHistoryTransaction, false);
          view.dispatch(tr);
        }

        // if this is a dismiss of an omni_insert mark then the command
        // isn't part of 'natural' typing into the document, so remove it
      } else if (isOmniInsertCommandActive(view.state.selection)) {
        removeCommandText();
      }
    },

    view: {
      component: OmniInserterView,
      key: command => command.id,
      width: 320,
      height: 46,
      maxVisible: 5,
    },
  };
}

const kOmniInsertRegex = /\/([\w]*)$/;

function omniInsertCompletions(omniInserters: OmniInserter[], ui: EditorUI) {
  return (text: string, doc: ProsemirrorNode, selection: Selection): CompletionResult<OmniInserter> | null => {
    const match = text.match(kOmniInsertRegex);
    if (match) {
      // we need to either be at the beginning of our parent, OR the omni_insert mark needs
      // to be active (that indicates that we entered omni insert mode via a user command)
      if (match.index !== 0 && !isOmniInsertCommandActive(selection)) {
        return null;
      }

      // capture query (note that no query returns all).
      const query = match[1].toLowerCase();

      // include a decoration if the query is empty
      const decorations =
        query.length === 0
          ? DecorationSet.create(doc, [
              placeholderDecoration(selection.head, ui.context.translateText(' Type to search...')),
            ])
          : undefined;

      // return the completion handler
      return {
        // match at the /
        pos: selection.head - match[0].length,

        // look through registered onmi inserters for completions
        completions: (state: EditorState) => {
          // match contents of name or keywords (and verify the command is enabled)
          const inserters = omniInserters
            .filter(inserter => {
              return (
                query.length === 0 ||
                inserter.name.toLowerCase().indexOf(query) !== -1 ||
                inserter.keywords?.some(keyword => keyword.indexOf(query) !== -1)
              );
            })
            .filter(inserter => {
              return inserter.command(state);
            })
            .sort(
              firstBy(omniInsertGroupCompare)
                .thenBy(omniInsertPriorityCompare, { direction: 'desc' })
                .thenBy('name'),
            );

          // resolve prosmie
          return Promise.resolve(inserters);
        },

        // search placehodler decorator if there is no query
        decorations,
      };
    } else {
      return null;
    }
  };
}

const OmniInserterView: React.FC<OmniInserter> = inserter => {
  return (
    <table className={'pm-omni-insert-completion'}>
      <tbody>
        <tr>
          <td className={'pm-omni-insert-icon'}>
            <img src={inserter.image()} alt="" />
          </td>
          <td>
            <div className={'pm-omni-insert-name pm-completion-item-text'}>{inserter.name}</div>
            <div className={'pm-omni-insert-description pm-completion-item-text'}>{inserter.description}</div>
          </td>
        </tr>
      </tbody>
    </table>
  );
};

function isOmniInsertCommandActive(selection: Selection) {
  const schema = selection.$head.parent.type.schema;
  return schema.marks.omni_insert.isInSet(selection.$head.marks());
}
