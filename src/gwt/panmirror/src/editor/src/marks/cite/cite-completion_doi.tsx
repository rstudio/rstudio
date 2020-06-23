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

import { EditorUI, InsertBibEntryProps } from '../../api/ui';
import { CrossrefServer, parseDOI, CrossrefWork } from '../../api/crossref';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { parseCitation } from './cite-completion';
import { imageForType, formatAuthors, formatIssuedDate, suggestId } from './cite-bibliography_entry';
import { CompletionItemDetailedView } from '../../api/widgets/completion-detailed';
import { performCompletionReplacement } from '../../behaviors/completion/completion';

export function citationDoiCompletionHandler(ui: EditorUI, server: CrossrefServer): CompletionHandler<CrossrefEntry> {
  return {
    id: '56DA14DD-6E3A-4481-93A9-938DC00393A5',

    completions: citationDOICompletions(ui, server),

    replace(view: EditorView, pos: number, completion: CrossrefEntry | null) {
      if (completion) {
        const bibProps: InsertBibEntryProps = {
          suggestedId: suggestId(completion.author, completion.issued),
          bibliographyFiles: [],
        };

        // Use the biblography manager to write an entry to the user specified bibliography

        // Ask the user to provide information that we need in order to populate
        // this citation (id, bibliography)
        const citation = ui.dialogs.insertBibEntry(bibProps).then(result => {
          // If the user provided an id, insert the citation
          if (result && result.id.length) {
            performCompletionReplacement(view, pos, result.id);
          }
        });
      }
    },

    view: {
      component: CrossrefWorkView,
      key: work => work.DOI,
      width: 400,
      height: 120,
      maxVisible: 5,
      hideNoResults: true,
    },
  };
}

function citationDOICompletions(ui: EditorUI, server: CrossrefServer) {
  return (_text: string, context: EditorState | Transaction): CompletionResult<CrossrefEntry> | null => {
    const parsed = parseCitation(context);
    if (parsed) {
      const doi = parseDOI(parsed.token);
      if (doi) {
        return {
          token: parsed.token,
          pos: parsed.pos,
          offset: parsed.offset,
          completions: (_state: EditorState) =>
            server.doi(parsed.token).then(work => [
              {
                ...work,
                image: imageForType(ui, work.type)[ui.prefs.darkMode() ? 1 : 0],
                formattedAuthor: formatAuthors(work.author, 50),
                formattedIssueDate: formatIssuedDate(work.issued, ui),
              },
            ]),
        };
      }
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
      width={400}
      image={work.image}
      heading={work['short-container-title'] || work.publisher}
      title={work.title[0]}
      subTitle={`${work.formattedAuthor} ${work.formattedIssueDate}` || ''}
    />
  );
};
