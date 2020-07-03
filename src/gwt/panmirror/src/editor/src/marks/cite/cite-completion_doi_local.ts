/*
 * cite-completion_coi_local.ts
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
import { EditorState, Transaction } from "prosemirror-state";

import { EditorUI } from "../../api/ui";
import { BibliographyManager } from "../../api/bibliography";
import { CompletionHandler, CompletionResult } from "../../api/completion";
import { BibliographyEntry, entryForSource } from "./cite-bibliography_entry";
import { kCitationCompleteScope, kCiteCompletionWidth, BibliographySourceView } from "./cite-completion";
import { parseCitation } from "./cite";
import { hasDOI } from '../../api/doi';

export function citationLocalDoiCompletionHandler(
  ui: EditorUI,
  bibManager: BibliographyManager,
): CompletionHandler<BibliographyEntry> {
  return {
    id: '9720F731-BD06-4C39-904B-2C6AB5B4BD99',

    scope: kCitationCompleteScope,

    completions: citationLocaLDOICompletions(ui, bibManager),

    replacement(_schema: Schema, entry: BibliographyEntry | null): string | ProsemirrorNode | null {
      if (entry) {
        return entry.source.id;
      } else {
        return null;
      }
    },

    view: {
      component: BibliographySourceView,
      key: entry => entry.source.id,
      width: kCiteCompletionWidth,
      height: 54,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

function citationLocaLDOICompletions(ui: EditorUI, manager: BibliographyManager) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<BibliographyEntry> | null => {
    const parsed = parseCitation(context);
    if (parsed && hasDOI(parsed.token)) {
      // TODO: This could be called before the bibliography is downloaded
      // And then items won't be loaded and duplicate DOIs will be missed
      const source = manager.findDoi(parsed.token);
      if (source) {
        return {
          token: parsed.token,
          pos: parsed.pos,
          offset: parsed.offset,
          completions: (_state: EditorState) => {
            return Promise.resolve([entryForSource(source, ui)]);
          }
        };
      }
    }
    return null;
  };
}
