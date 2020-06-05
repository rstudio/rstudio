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

import { EditorState, Selection } from "prosemirror-state";
import { EditorView } from "prosemirror-view";

import React from 'react';

import { OmniInserter } from "../../api/omni_insert";
import { CompletionHandler, CompletionResult } from "../../api/completion";

export function omniInsertCompletionHandler(omniInserters: OmniInserter[]) : CompletionHandler<OmniInserter> {

  return {

    completions: omniInsertCompletions(omniInserters),

    replace(view: EditorView, pos: number, completion: OmniInserter | null) {

      // helper to remove command text
      const removeCommandText = () => {
         const tr = view.state.tr;
         tr.deleteRange(pos, view.state.selection.head);
         view.dispatch(tr);
      };

      // execute command if provided
      if (completion) {
        removeCommandText();
        completion.command(view.state, view.dispatch, view);

      // if this is a dismiss of an omni_insert mark then the command
      // isn't part of 'natural' typing into the document, so remove it
      } else if (isOmniInsertCommandActive(view.state.selection)) {
        removeCommandText();
      }
    },
    

    view: {
      component: OmniInserterView,
      key: command => command.id,
      width: 200
    },

  };

}

const kOmniInsertRegex = /\/([\w]*)$/;

function omniInsertCompletions(omniInserters: OmniInserter[]) {

  return (text: string, selection: Selection): CompletionResult<OmniInserter> | null => {

    const match = text.match(kOmniInsertRegex);
    if (match) {

      // we need to either be at the beginning of our parent, OR the omni_insert mark needs
      // to be active (that indicates that we entered omni insert mode via a user command)
      const parent = selection.$head.parent;
      if (match.index !== 0 && !isOmniInsertCommandActive(selection)) {
        return null;
      }

      // capture query (note that no query returns all)
      const query = match[1].toLowerCase();

      return {
        pos: selection.head - match[0].length,  
        completions: (state: EditorState) => {

          // match contents of name or keywords (and verify the command is enabled)
          const inserters = omniInserters
            .filter(inserter => {
              return query.length === 0 ||
                     inserter.name.toLowerCase().indexOf(query) !== -1 ||
                     inserter.keywords?.some(keyword => keyword.indexOf(query) !== -1);
              })
            .filter(inserter => {
               return inserter.command(state);
            });

          // resolve prosmie
          return Promise.resolve(inserters);
        }
      };
    } else {
      return null;
    }

  };

}

const OmniInserterView: React.FC<OmniInserter> = command => {
  return (
    <div className={'pm-completion-item-text'}>
      {command.name}&nbsp;:{command.description}:
    </div>
  );
};

function isOmniInsertCommandActive(selection: Selection) {
  const schema = selection.$head.parent.type.schema;
  return schema.marks.omni_insert.isInSet(selection.$head.marks());
}




