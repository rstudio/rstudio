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

import { EditorState, Transaction, Selection } from "prosemirror-state";
import { EditorView } from "prosemirror-view";

import React from 'react';

import { CompletionHandler, CompletionResult } from "../api/completion";
import { OmniInserter } from "../api/omni_insert";
import { ProsemirrorCommand, EditorCommandId } from "../api/command";


// TODO: very first Esc key for omni insert is ignored

// TODO: do we really need the schema to build the list of omniInserters or 
// do we just need one more level of callback?


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

        // remove completion command text
        removeCommandText();

        completion.command(view.state, view.dispatch, view);
      } else {

        // TODO: remove text ONLY if it was inserted as a special artifact
        // removeCommandText();

      }

    },
    

    view: {
      component: OmniInserterView,
      key: command => command.id,
      width: 200
    },

  };

}

const kOmniInsertRegex = /^\/([\w]*)$/;

function omniInsertCompletions(omniInserters: OmniInserter[]) {

  return (text: string, selection: Selection): CompletionResult<OmniInserter> | null => {

    const match = text.match(kOmniInsertRegex);
    if (match) {
      const query = match[1];
      return {
        pos: selection.head - match[0].length,  
        completions: (state: EditorState) => {
          return Promise.resolve(omniInserters);
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


const extension = {
  commands: () => [new OmniInsertCommand()]
};

class OmniInsertCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.OmniInsert, ['Mod-/'], (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
          
      if (dispatch) {
        // 
      }

      return true;
    });
  }
}

export default extension;



