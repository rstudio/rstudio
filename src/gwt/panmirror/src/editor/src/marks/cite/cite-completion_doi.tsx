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
import { CrossrefServer, CrossrefWork } from '../../api/crossref';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { kCitationCompleteScope } from './cite-completion';
import { imageForType, formatAuthors, formatIssuedDate } from './cite-bibliography_entry';
import { CompletionItemDetailedView } from '../../api/widgets/completion-detailed';
import { BibliographyManager } from '../../api/bibliography';
import { EditorServer } from '../../api/server';

import { parseDOI } from './cite-doi';
import { insertCitationForDOI } from './cite';

const kCompletionWidth = 400;
const kCompletionItemPadding = 10;

export function citationDoiCompletionHandler(
  ui: EditorUI,
  bibManager: BibliographyManager,
  server: EditorServer,
): CompletionHandler<CrossrefEntry> {
  return {
    id: '56DA14DD-6E3A-4481-93A9-938DC00393A5',

    scope: kCitationCompleteScope,

    completions: citationDOICompletions(ui, server.crossref),

    replace(view: EditorView, pos: number, work: CrossrefEntry | null) {
      if (work) {
        insertCitationForDOI(view, work.DOI, bibManager, pos, ui, server.pandoc, work);
      }
    },

    view: {
      component: CrossrefWorkView,
      key: work => work.DOI,
      width: kCompletionWidth,
      height: 120,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

function citationDOICompletions(ui: EditorUI, server: CrossrefServer) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<CrossrefEntry> | null => {
    const parsedDOI = parseDOI(context);
    if (parsedDOI) {
      return {
        token: parsedDOI.token,
        pos: parsedDOI.pos,
        offset: parsedDOI.offset,
        completions: (_state: EditorState) =>
          server.doi(parsedDOI.token, 350).then(work => [
            {
              ...work,
              image: imageForType(ui, work.type)[ui.prefs.darkMode() ? 1 : 0],
              formattedAuthor: formatAuthors(work.author, 50),
              formattedIssueDate: formatIssuedDate(work.issued),
            },
          ]),
      };
    }
    return null;
  };
}

interface CrossrefEntry extends CrossrefWork {
  image?: string;
  formattedAuthor: string;
  formattedIssueDate: string;
}

// The title may contain spans to control case specifically - consequently, we need
// to render the title as HTML rather than as a string
const CrossrefWorkView: React.FC<CrossrefEntry> = work => {
  return (
    <CompletionItemDetailedView
      width={kCompletionWidth - kCompletionItemPadding}
      image={work.image}
      heading={work['short-container-title'] || work.publisher}
      title={work.title[0]}
      subTitle={`${work.formattedAuthor} ${work.formattedIssueDate}` || ''}
    />
  );
};
