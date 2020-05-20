/*
 * rmd_chunk-commands.ts
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

import { ProsemirrorCommand, EditorCommandId } from "../../api/command";
import { EditorUI } from "../../api/ui";
import { EditorState, Transaction } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { activeRmdChunk, mergeRmdChunks, previousExecutableRmdChunks } from "../../api/rmd";


export class ExecuteCurrentRmdChunkCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI) {
    super(
      EditorCommandId.ExecuteCurentRmdChunk,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void) => {
      
        if (!ui.execute.executeRmdChunk) {
          return false;
        }

        const chunk = activeRmdChunk(state);
        if (!chunk) {
          return false;
        }

        if (dispatch) {
          ui.execute.executeRmdChunk(chunk);
        }

        return true;
      },
    );
  }
}

export class ExecutePreviousRmdChunksCommand extends ProsemirrorCommand {
  constructor(ui: EditorUI) {
    super(
      EditorCommandId.ExecuteCurrentPreviousRmdChunks,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
      
        if (!ui.execute.executeRmdChunk) {
          return false;
        }

        if (dispatch) {
          const prevChunks = mergeRmdChunks(previousExecutableRmdChunks(state));
          if (prevChunks) {
            ui.execute.executeRmdChunk(prevChunks);
          }
        }

        return true;
      },
    );
  }
}

