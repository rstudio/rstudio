/*
 * tab.ts
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

import { EditorState, Transaction } from "prosemirror-state";
import { EditorView } from "prosemirror-view";

import { CommandFn } from "./command";
import { EditorUIPrefs } from "./ui";


// wrap a tab key command such that it doesn't apply when the tabKeyMoveFocus pref is active
export function tabKeyCommand(prefs: EditorUIPrefs, command: CommandFn) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
    if (prefs.tabKeyMoveFocus()) {
      return false;
    } else {
      return command(state, dispatch, view);
    }
  };
}

