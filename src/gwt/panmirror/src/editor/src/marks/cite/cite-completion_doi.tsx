/*
 * cite-completion_doi.tsx
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


import { EditorView } from 'prosemirror-view';
import { EditorState, Transaction } from 'prosemirror-state';

import React from 'react';

import { EditorUI } from '../../api/ui';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { kCitationCompleteScope } from './cite-completion';
import { imageForType, formatAuthors, formatIssuedDate } from './cite-bibliography_entry';
import { CompletionItemDetailedView } from '../../api/widgets/completion-detailed';
import { BibliographyManager } from '../../api/bibliography';
import { EditorServer } from '../../api/server';
import { DOIServer } from '../../api/doi';

import { parseDOI } from './cite-doi';
import { insertCitationForDOI } from './cite';
import { CSL } from '../../api/csl';

const kCompletionWidth = 400;
const kCompletionItemPadding = 10;

export function citationDoiCompletionHandler(
  ui: EditorUI,
  bibManager: BibliographyManager,
  server: EditorServer,
): CompletionHandler<CSLEntry> {
  return {
    id: '56DA14DD-6E3A-4481-93A9-938DC00393A5',

    scope: kCitationCompleteScope,

    completions: citationDOICompletions(ui, server.doi),

    replace(view: EditorView, pos: number, cslEntry: CSLEntry | null) {
      if (cslEntry && cslEntry.csl.DOI) {
        insertCitationForDOI(view, cslEntry.csl.DOI, bibManager, pos, ui, server.pandoc, cslEntry.csl);
      }
    },

    view: {
      component: CSLSourceView,
      key: cslEntry => cslEntry.csl.DOI,
      width: kCompletionWidth,
      height: 120,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

const kPRogressDelay = 350;

function citationDOICompletions(ui: EditorUI, server: DOIServer) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<CSLEntry> | null => {
    const parsedDOI = parseDOI(context);
    if (parsedDOI) {
      return {
        token: parsedDOI.token,
        pos: parsedDOI.pos,
        offset: parsedDOI.offset,
        completions: (_state: EditorState) => {
          return server.fetchCSL(parsedDOI.token, kPRogressDelay).then(result => {
            if (result.status === "ok") {
              const csl = result.message;

              // We should only return csl that includes a DOI since this UI depends upon that being present
              // Note that this should always be true because we are looking up the CSL by DOI, but in the event
              // that the server returns something unexpected, we will simply not match any completions
              if (csl.DOI) {
                return [
                  {
                    id: csl.DOI,
                    csl,
                    image: imageForType(ui, csl.type)[ui.prefs.darkMode() ? 1 : 0],
                    formattedAuthor: formatAuthors(csl.author, 50),
                    formattedIssueDate: formatIssuedDate(csl.issued),
                  },
                ];
              } else {
                return [];
              }
            } else if (result.status === "notfound" || result.status === "nohost") {
              return [];
            } else if (result.status === "error") {
              // TODO: deal w/ error? (it's already been logged on the server)
              return [];
            } else {
              return [];
            }
          });
        }
      };
    }
    return null;
  };
}

interface CSLEntry {
  id: string;
  csl: CSL;
  image?: string;
  formattedAuthor: string;
  formattedIssueDate: string;
}

// The title may contain spans to control case specifically - consequently, we need
// to render the title as HTML rather than as a string
const CSLSourceView: React.FC<CSLEntry> = cslEntry => {
  const csl = cslEntry.csl;
  return (
    <CompletionItemDetailedView
      width={kCompletionWidth - kCompletionItemPadding}
      image={cslEntry.image}
      heading={csl['short-container-title'] || csl.publisher || ''}
      title={csl.title || ''}
      subTitle={`${cslEntry.formattedAuthor} ${cslEntry.formattedIssueDate}` || ''}
    />
  );
};
