

/*
 * omni_command.ts
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

import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { EditorState, Transaction, Selection } from "prosemirror-state";
import { EditorView } from "prosemirror-view";

import React from 'react';

import { ProsemirrorCommand, EditorCommandId, OmniCommand } from "../api/command";
import { Extension } from "../api/extension";
import { CompletionHandler, CompletionResult } from "../api/completion";


export function omniCommandExtension(omniCommands: OmniCommand[]) : Extension {
  return {
    completionHandlers: () => [omniCommandCompletionHandler(omniCommands)],
    commands: () => [new OmniCompletionCommand()]
  };
}


function omniCommandCompletionHandler(omniCommands: OmniCommand[]) : CompletionHandler<OmniCommand> {

  return {

    completions: omniCommandCompletions(omniCommands),

    replacement(schema: Schema, command: OmniCommand) : string | ProsemirrorNode {
      
      return 'foo';
    },

    view: {
      component: OmniCommandView,
      key: command => command.id,
      width: 200
    },

  };

}

const kOmniCommandRegex = /^\/([\w ]*)$/;

function omniCommandCompletions(omniCommands: OmniCommand[]) {

  return (text: string, selection: Selection): CompletionResult<OmniCommand> | null => {

    const match = text.match(kOmniCommandRegex);
    if (match) {
      const query = match[1];
      return {
        pos: selection.head - match[0].length,  
        completions: (state: EditorState) => {
          return Promise.resolve(omniCommands);
        }
      };
    } else {
      return null;
    }

  };

}

const OmniCommandView: React.FC<OmniCommand> = command => {
  return (
    <div className={'pm-completion-item-text'}>
      {command.name}&nbsp;:{command.description}:
    </div>
  );
};


class OmniCompletionCommand extends ProsemirrorCommand {
  constructor() {
    super(EditorCommandId.Omni, ['Mod-/'], (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
          
      if (dispatch) {
        // 
      }
  
      return true;
    });
  }

}





